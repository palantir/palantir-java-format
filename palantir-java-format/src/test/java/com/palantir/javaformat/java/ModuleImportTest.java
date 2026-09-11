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
    public void keepsACommentBetweenModuleAndTheModuleName() throws FormatterException {
        String input = "import module /* comment */ java.base;\n" + "class Example {}\n";
        String expected = "import module java.base; /* comment */\n" + "\n" + "class Example {}\n";
        assertFormats(input, expected);
    }

    @Test
    public void acceptsALineBreakAfterModule() throws FormatterException {
        String input = "import module\n" + "    java.base;\n" + "class Example {}\n";
        String expected = "import module java.base;\n" + "\n" + "class Example {}\n";
        assertFormats(input, expected);
    }
}
