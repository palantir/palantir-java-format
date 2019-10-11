/*
 * Copyright 2019 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static com.palantir.javaformat.java.FileBasedTests.isRecreate;

import java.io.IOException;
import java.util.Collection;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** {@link StringWrapper}IntegrationTest */
@RunWith(Parameterized.class)
public class StringWrapperIntegrationTest {

    private static FileBasedTests tests = new FileBasedTests(StringWrapperIntegrationTest.class);

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> parameters() throws IOException {
        return tests.paramsAsNameInputOutput();
    }

    private final Formatter formatter = new Formatter();

    private final String name;
    private final String input;
    private final String output;

    public StringWrapperIntegrationTest(String name, String input, String output) {
        this.name = name;
        this.input = input;
        this.output = output;
    }

    @Test
    public void test() throws Exception {
        String actualOutput = StringWrapper.wrap(40, formatter.formatSource(input), formatter);
        if (isRecreate()) {
            tests.writeFormatterOutput(name, actualOutput);
        } else {
            assertThat(actualOutput).isEqualTo(output);
        }
    }

    @Test
    public void testCR() throws Exception {
        Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
        assertThat(StringWrapper.wrap(40, formatter.formatSource(input.replace("\n", "\r")), formatter))
                .isEqualTo(output.replace("\n", "\r"));
    }

    @Test
    public void testCRLF() throws Exception {
        Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
        assertThat(StringWrapper.wrap(40, formatter.formatSource(input.replace("\n", "\r\n")), formatter))
                .isEqualTo(output.replace("\n", "\r\n"));
    }

    @Test
    public void idempotent() throws Exception {
        Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
        String wrap = StringWrapper.wrap(40, formatter.formatSource(input), formatter);
        assertThat(formatter.formatSource(wrap)).isEqualTo(wrap);
    }
}
