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

package com.palantir.javaformat.gradle;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class SupportsMissingSpotlessTest {

    @Test
    void can_be_applied_when_spotless_is_not_on_the_classpath() {
        assertThatThrownBy(() -> Class.forName("com.diffplug.gradle.spotless.SpotlessExtension"))
                .as("spotless must be off the test classpath for this test to be meaningful")
                .isInstanceOf(ClassNotFoundException.class);

        Project project = ProjectBuilder.builder().build();

        assertThatCode(() -> project.getPluginManager().apply(PalantirJavaFormatSpotlessPlugin.class))
                .doesNotThrowAnyException();
    }
}
