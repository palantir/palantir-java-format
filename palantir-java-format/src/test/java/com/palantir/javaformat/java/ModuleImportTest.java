/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.javaformat.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for module import declarations (JEP 511) through the pipeline the Gradle plugin and Spotless use,
 * which reorders imports before formatting. The {@code ModuleImport} golden only exercises {@code formatSource}.
 */
public class ModuleImportTest {

    @BeforeAll
    public static void requiresAParserThatProducesModuleImports() {
        // Module imports parse from JDK 23 on, as a preview feature there; the formatter enables preview.
        Assumptions.assumeTrue(
                Formatter.getRuntimeVersion() >= 23, "import module requires running on JDK 23 or later");
    }

    /** Asserts both entry points that reorder imports, and that their output survives a second pass. */
    private static void assertFormats(String input, String expected) throws FormatterException {
        assertThat(Formatter.create().formatSourceAndFixImports(input)).isEqualTo(expected);
        assertThat(Formatter.create().fixImports(input)).isEqualTo(expected);
        assertThat(Formatter.create().formatSourceAndFixImports(expected)).isEqualTo(expected);
        assertThat(Formatter.create().fixImports(expected)).isEqualTo(expected);
    }

    @Test
    public void formatsAndFixesImports() throws FormatterException {
        String input = "import module java.base;\n" + "class Example {}\n";
        String expected = "import module java.base;\n" + "\n" + "class Example {}\n";
        assertFormats(input, expected);
    }

    @Test
    public void fixesImportsOnlyFromTheCommandLine() throws Exception {
        // The flag combination from the #1506 report, which failed with `Expected ; after import`.
        String input = "import module java.base;\n" + "class Example {}\n";
        // Reordering puts a blank line after the import block; nothing else changes.
        String expected = "import module java.base;\n" + "\n" + "class Example {}\n";
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        Main main = new Main(
                new PrintWriter(out, true),
                new PrintWriter(err, true),
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        int exitCode = main.format("-", "--fix-imports-only", "--skip-removing-unused-imports");
        assertThat(err.toString()).isEmpty();
        assertThat(exitCode).isZero();
        assertThat(out.toString()).isEqualTo(expected);
    }

    @Test
    public void keepsACommentBetweenModuleAndTheModuleName() throws FormatterException {
        String input = "import module /* comment */ java.base;\n" + "class Example {}\n";
        String expected = "import module /* comment */ java.base;\n" + "\n" + "class Example {}\n";
        assertFormats(input, expected);
    }

    @Test
    public void keepsACommentBetweenThePartsOfTheModuleName() throws FormatterException {
        String input = "import module java./* comment */base;\n" + "class Example {}\n";
        // Reordering normalizes the whitespace around the comment and leaves it between the parts.
        String reordered = "import module java./* comment */ base;\n" + "\n" + "class Example {}\n";
        assertThat(Formatter.create().fixImports(input)).isEqualTo(reordered);
        assertThat(Formatter.create().fixImports(reordered)).isEqualTo(reordered);

        // Formatting then breaks the line after the dot, which is where the formatter puts a comment
        // in a qualified name; that output is stable too.
        String formatted = "import module java.\n" + "/* comment */ base;\n" + "\n" + "class Example {}\n";
        assertThat(Formatter.create().formatSourceAndFixImports(input)).isEqualTo(formatted);
        assertThat(Formatter.create().formatSourceAndFixImports(formatted)).isEqualTo(formatted);
    }

    @Test
    public void normalizesWhitespaceInsideTheModuleName() throws FormatterException {
        String input = "import module java . base;\n" + "class Example {}\n";
        String expected = "import module java.base;\n" + "\n" + "class Example {}\n";
        assertFormats(input, expected);
    }

    @Test
    public void keepsBothCopiesOfADuplicateThatCarriesAComment() throws FormatterException {
        // Identical declarations collapse; ones that differ are both kept, so no comment is dropped.
        String input = "import module /* explanation A */ java.base;\n"
                + "import module /* explanation B */ java.base;\n"
                + "class Example {}\n";
        String expected = "import module /* explanation A */ java.base;\n"
                + "import module /* explanation B */ java.base;\n"
                + "\n"
                + "class Example {}\n";
        assertFormats(input, expected);
    }

    @Test
    public void acceptsALineBreakAfterModule() throws FormatterException {
        String input = "import module\n" + "    java.base;\n" + "class Example {}\n";
        String expected = "import module java.base;\n" + "\n" + "class Example {}\n";
        assertFormats(input, expected);
    }
}
