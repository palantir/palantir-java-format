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
import com.palantir.platform.CurrentArch;
import com.palantir.platform.CurrentOs;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;

public final class PalantirJavaFormatProviderPlugin implements Plugin<Project> {

    static final String CONFIGURATION_NAME = "palantirJavaFormat";

    static final String NATIVE_CONFIGURATION_NAME = "palantirJavaFormatNative";

    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-provider to the root project");

        String implementationVersion = JavaFormatExtension.class.getPackage().getImplementationVersion();

        Configuration configuration = rootProject.getConfigurations().create(CONFIGURATION_NAME);
        configuration.setDescription("Internal configuration for resolving the palantir-java-format implementation");
        configuration.setVisible(false);
        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);
        configuration.defaultDependencies(deps -> {
            deps.add(rootProject
                    .getDependencies()
                    .create(ImmutableMap.of(
                            "group",
                            "com.palantir.javaformat",
                            "name",
                            "palantir-java-format",
                            "version",
                            implementationVersion)));
        });

        Configuration nativeConfiguration = rootProject.getConfigurations().create(NATIVE_CONFIGURATION_NAME);
        nativeConfiguration.setDescription(
                "Internal configuration for resolving the palantir-java-format implementation");
        nativeConfiguration.setVisible(false);
        nativeConfiguration.setCanBeConsumed(false);
        nativeConfiguration.setCanBeResolved(true);
        nativeConfiguration.defaultDependencies(deps -> {
            deps.add(rootProject
                    .getDependencies()
                    .create(ImmutableMap.of(
                            "group",
                            "com.palantir.javaformat",
                            "name",
                            "palantir-java-format-native",
                            "version",
                            implementationVersion,
                            "classifier",
                            String.format(
                                    "nativeImage-%s-%s",
                                    CurrentOs.get().uiName(), CurrentArch.get().uiName()),
                            "ext",
                            "exe")));
        });
        nativeConfiguration
                .getAttributes()
                .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        rootProject.getDependencies().registerTransform(ExecutableTransform.class, transformSpec -> {
            transformSpec.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "exe");
            transformSpec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        });

        rootProject.getExtensions().create("palantirJavaFormat", JavaFormatExtension.class, configuration);
    }
}
