/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.palantir.javaformat.java;

import static com.palantir.javaformat.java.FileBasedTests.assumeJavaVersionForTest;
import static com.palantir.javaformat.java.FileBasedTests.isRecreate;
import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javaformat.Newlines;
import com.palantir.javaformat.jupiter.ParameterizedClass;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@ExtendWith(ParameterizedClass.class)
@Execution(ExecutionMode.CONCURRENT)
public class FormatterIntegrationTest {

    private static FileBasedTests tests = new FileBasedTests(FormatterIntegrationTest.class, "testdata");

    @ParameterizedClass.Parameters(name = "{0}")
    public static List<Object[]> data() throws IOException {
        return tests.paramsAsNameInputOutput();
    }

    private final String name;
    private final String input;
    private final String expected;
    private final String separator;

    public FormatterIntegrationTest(String name, String input, String expected) {
        this.name = name;
        this.input = input;
        this.expected = expected;
        this.separator = isRecreate() ? null : Newlines.getLineEnding(expected);
    }

    /**
     * If enabled, then {@link DebugRenderer} will produce an output at {@link DebugRenderer#getOutputFile()}. This can
     * then be viewed in a browser by running the following once (after which it will auto-reload whenever the debug
     * output file changes):
     *
     * <pre>
     * cd debugger
     * yarn start
     * </pre>
     *
     * <p>Warning: don't turn this on for all tests. The debugger will always write to the same file.
     */
    private static boolean isDebugMode() {
        return Boolean.getBoolean("debugOutput");
    }

    @TestTemplate
    public void format() {
        assumeJavaVersionForTest(name);
        try {
            String output = createFormatter().formatSource(input);
            if (isRecreate()) {
                tests.writeFormatterOutput(name, output);
                return;
            }
            assertThat(output).describedAs("bad output for " + name).isEqualTo(expected);
        } catch (FormatterException e) {
            throw new RuntimeException(String.format("Formatter crashed on %s", name), e);
        }
    }

    private static Formatter createFormatter() {
        return new Formatter(
                JavaFormatterOptions.builder()
                        .style(JavaFormatterOptions.Style.PALANTIR)
                        .build(),
                isDebugMode(),
                JavaFormatterInternalOptions.builder().build());
    }

    @TestTemplate
    public void idempotentLF() {
        assumeJavaVersionForTest(name);
        Assumptions.assumeFalse(isRecreate(), "Not running when recreating test outputs");
        try {
            String mangled = expected.replace(separator, "\n");
            String output = createFormatter().formatSource(mangled);
            assertThat(output).describedAs("bad output for " + name).isEqualTo(mangled);
        } catch (FormatterException e) {
            throw new RuntimeException(String.format("Formatter crashed on %s", name), e);
        }
    }

    @TestTemplate
    public void idempotentCR() {
        assumeJavaVersionForTest(name);
        Assumptions.assumeFalse(isRecreate(), "Not running when recreating test outputs");
        try {
            String mangled = expected.replace(separator, "\r");
            String output = createFormatter().formatSource(mangled);
            assertThat(output).describedAs("bad output for " + name).isEqualTo(mangled);
        } catch (FormatterException e) {
            throw new RuntimeException(String.format("Formatter crashed on %s", name), e);
        }
    }

    @TestTemplate
    public void idempotentCRLF() {
        assumeJavaVersionForTest(name);
        Assumptions.assumeFalse(isRecreate(), "Not running when recreating test outputs");
        try {
            String mangled = expected.replace(separator, "\r\n");
            String output = createFormatter().formatSource(mangled);
            assertThat(output).describedAs("bad output for " + name).isEqualTo(mangled);
        } catch (FormatterException e) {
            throw new RuntimeException(String.format("Formatter crashed on %s", name), e);
        }
    }
}
