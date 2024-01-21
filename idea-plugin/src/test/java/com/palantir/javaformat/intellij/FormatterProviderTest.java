/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.javaformat.intellij;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public final class FormatterProviderTest {

    @Test
    void testParseSdkJavaVersion_major() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15")).hasValue(15);
    }

    @Test
    void testParseSdkJavaVersion_majorMinorPatch() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15.0.2")).hasValue(15);
    }

    @Test
    void testParseSdkJavaVersion_ea() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15-ea")).hasValue(15);
    }

    @Test
    void testParseSdkJavaVersion_invalidVersion_isEmpty() {
        assertThat(FormatterProvider.parseSdkJavaVersion("not-a-version")).isEmpty();
    }
}
