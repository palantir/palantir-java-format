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
import com.google.common.collect.ImmutableList;
import com.palantir.gradle.ideaconfiguration.IdeaConfigurationExtension;
import com.palantir.gradle.ideaconfiguration.IdeaConfigurationPlugin;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskProvider;

public abstract class PalantirJavaFormatIdeaPlugin implements Plugin<Project> {

    @Nested
    protected abstract NativeImageSupport getNativeImageSupport();

    @Inject
    protected abstract ConfigurationContainer getConfigurations();

    private static final String MIN_IDEA_PLUGIN_VERSION = "2.57.0";

    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-idea to the root project");

        rootProject.getPlugins().apply(PalantirJavaFormatProviderPlugin.class);
        rootProject.getPluginManager().withPlugin("idea", ideaPlugin -> {
            TaskProvider<UpdatePalantirJavaFormatIdeaXmlFile> updatePalantirJavaFormatXml = rootProject
                    .getTasks()
                    .register("updatePalantirJavaFormatXml", UpdatePalantirJavaFormatIdeaXmlFile.class, task -> {
                        task.getXmlOutputFile().set(rootProject.file(".idea/palantir-java-format.xml"));
                        task.getImplementationConfig()
                                .from(rootProject
                                        .getConfigurations()
                                        .getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME));
                        maybeGetNativeImplConfiguration().ifPresent(config -> {
                            task.getNativeImageConfig().from(config);
                            task.getNativeImageOutputFile().fileProvider(rootProject.provider(() -> rootProject
                                    .getGradle()
                                    .getGradleUserHomeDir()
                                    .toPath()
                                    .resolve("palantir-java-format-caches/")
                                    .resolve(Paths.get(task.getNativeImageConfig()
                                                    .getSingleFile()
                                                    .toURI())
                                            .getFileName()
                                            .toString())
                                    .toFile()));
                        });
                    });

            TaskProvider<UpdateWorkspaceXmlFile> updateWorkspaceXml = rootProject
                    .getTasks()
                    .register("updateWorkspaceXml", UpdateWorkspaceXmlFile.class, task -> {
                        task.getOutputFile().set(rootProject.file(".idea/workspace.xml"));
                    });

            // Add the task to the Gradle start parameters so it executes automatically.
            StartParameter startParameter = rootProject.getGradle().getStartParameter();
            List<String> updateTasks = Stream.of(updatePalantirJavaFormatXml, updateWorkspaceXml)
                    .map(taskProvider -> String.format(":%s", taskProvider.getName()))
                    .toList();
            List<String> taskNames = ImmutableList.<String>builder()
                    .addAll(startParameter.getTaskNames())
                    .addAll(updateTasks)
                    .build();
            startParameter.setTaskNames(taskNames);
        });

        rootProject.getPluginManager().apply(IdeaConfigurationPlugin.class);
        IdeaConfigurationExtension extension = rootProject.getExtensions().getByType(IdeaConfigurationExtension.class);
        extension
                .getExternalDependencies()
                .register("palantir-java-format", dep -> dep.atLeastVersion(MIN_IDEA_PLUGIN_VERSION));
    }

    private Optional<Configuration> maybeGetNativeImplConfiguration() {
        return getNativeImageSupport().isNativeImageConfigured()
                ? Optional.of(getConfigurations().getByName(NativeImageFormatProviderPlugin.NATIVE_CONFIGURATION_NAME))
                : Optional.empty();
    }
}
