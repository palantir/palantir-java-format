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

import static com.palantir.javaformat.java.FileBasedTests.isRecreate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.palantir.javaformat.Newlines;
import java.io.IOException;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Integration test for google-java-format. */
@RunWith(Parameterized.class)
public class FormatterIntegrationTest {

    private static FileBasedTests tests = new FileBasedTests(FormatterIntegrationTest.class, "testdata");

    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
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

    @Test
    public void format() {
        try {
            String output = createFormatter().formatSource(input);
            if (isRecreate()) {
                tests.writeFormatterOutput(name, output);
                return;
            }
            assertEquals("bad output for " + name, expected, output);
        } catch (FormatterException e) {
            fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
        }
    }

    private static Formatter createFormatter() {
        return new Formatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());
    }

    @Test
    public void idempotentLF() {
        Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
        try {
            String mangled = expected.replace(separator, "\n");
            String output = createFormatter().formatSource(mangled);
            assertEquals("bad output for " + name, mangled, output);
        } catch (FormatterException e) {
            fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
        }
    }

    @Test
    public void idempotentCR() {
        Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
        try {
            String mangled = expected.replace(separator, "\r");
            String output = createFormatter().formatSource(mangled);
            assertEquals("bad output for " + name, mangled, output);
        } catch (FormatterException e) {
            fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
        }
    }

    @Test
    public void idempotentCRLF() {
        Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
        try {
            String mangled = expected.replace(separator, "\r\n");
            String output = createFormatter().formatSource(mangled);
            assertEquals("bad output for " + name, mangled, output);
        } catch (FormatterException e) {
            fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
        }
    }
}
