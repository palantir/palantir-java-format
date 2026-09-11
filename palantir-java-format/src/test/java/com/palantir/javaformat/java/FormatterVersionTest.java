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

import org.junit.jupiter.api.Test;

/**
 * Guards the {@code testJdkNN} legs: without this, a leg that resolves to the wrong JDK skips every
 * version-gated golden test and still passes.
 */
public class FormatterVersionTest {

    @Test
    public void runsOnTheJdkTheTestTaskAskedFor() {
        String expected = System.getProperty("expectedJavaVersion");
        if (expected == null) {
            return; // the default `test` task doesn't set it
        }
        assertThat(Formatter.getRuntimeVersion()).isEqualTo(Integer.parseInt(expected));
    }
}
