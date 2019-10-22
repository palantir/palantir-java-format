/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PalantirJavaFormatProviderPlugin implements Plugin<Project> {

    static final String CONFIGURATION_NAME = "palantirJavaFormat";

    @Override
    public void apply(Project project) {
        Preconditions.checkState(
                project == project.getRootProject(),
                "May only apply com.palantir.java-format-provider to the root project");

        JavaFormatExtension extension =
                project.getExtensions().create("palantirJavaFormat", JavaFormatExtension.class, project);

        project.getConfigurations().create(CONFIGURATION_NAME, conf -> {
            conf.setDescription("Internal configuration for resolving the palantir-java-format implementation");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
            // Using addLater instead of afterEvaluate, in order to delay reading the extension until after the user
            // has configured it.
            conf.defaultDependencies(deps -> deps.addLater(project.provider(() -> {
                String version = extension.getImplementationVersion().get();

                return project.getDependencies().create(ImmutableMap.of(
                        "group", "com.palantir.javaformat",
                        "name", "palantir-java-format",
                        "version", version));
            })));
        });
    }
}
