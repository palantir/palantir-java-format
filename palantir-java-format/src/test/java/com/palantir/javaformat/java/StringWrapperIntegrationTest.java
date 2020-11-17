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

import com.palantir.javaformat.jupiter.ParameterizedClass;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.palantir.javaformat.java.FileBasedTests.assumeJava14ForJava14Tests;
import static com.palantir.javaformat.java.FileBasedTests.isRecreate;

@ExtendWith(ParameterizedClass.class)
@Execution(ExecutionMode.CONCURRENT)
public class StringWrapperIntegrationTest {

    private static FileBasedTests tests = new FileBasedTests(StringWrapperIntegrationTest.class);

    @ParameterizedClass.Parameters(name = "{0}")
    public static List<Object[]> parameters() throws IOException {
        return tests.paramsAsNameInputOutput();
    }

    private final Formatter formatter = Formatter.create();

    private final String name;
    private final String input;
    private final String output;

    public StringWrapperIntegrationTest(String name, String input, String output) {
        this.name = name;
        this.input = input;
        this.output = output;
    }

    @TestTemplate
    public void test() throws Exception {
        assumeJava14ForJava14Tests(name);
        String actualOutput = StringWrapper.wrap(40, formatter.formatSource(input), formatter);
        if (isRecreate()) {
            tests.writeFormatterOutput(name, actualOutput);
        } else {
            assertThat(actualOutput).isEqualTo(output);
        }
    }

    @TestTemplate
    public void testCR() throws Exception {
        assumeJava14ForJava14Tests(name);
        Assumptions.assumeFalse(isRecreate(), "Not running when recreating test outputs");
        assertThat(StringWrapper.wrap(40, formatter.formatSource(input.replace("\n", "\r")), formatter))
                .isEqualTo(output.replace("\n", "\r"));
    }

    @TestTemplate
    public void testCRLF() throws Exception {
        assumeJava14ForJava14Tests(name);
        Assumptions.assumeFalse(isRecreate(), "Not running when recreating test outputs");
        assertThat(StringWrapper.wrap(40, formatter.formatSource(input.replace("\n", "\r\n")), formatter))
                .isEqualTo(output.replace("\n", "\r\n"));
    }

    @TestTemplate
    public void idempotent() throws Exception {
        assumeJava14ForJava14Tests(name);
        Assumptions.assumeFalse(isRecreate(), "Not running when recreating test outputs");
        String wrap = StringWrapper.wrap(40, formatter.formatSource(input), formatter);
        assertThat(formatter.formatSource(wrap)).isEqualTo(wrap);
    }
}
