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
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.TaskTriggersConfig;

public final class PalantirJavaFormatIdeaPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Preconditions.checkState(
                project == project.getRootProject(),
                "May only apply com.palantir.java-format-idea to the root project");

        project.getPlugins().apply(PalantirJavaFormatProviderPlugin.class);

        project.getPluginManager().withPlugin("idea", ideaPlugin -> {
            Configuration implConfiguration =
                    project.getConfigurations().getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME);

            configureLegacyIdea(project, implConfiguration);
            configureIntelliJImport(project, implConfiguration);
        });
    }

    private static void configureLegacyIdea(Project project, Configuration implConfiguration) {
        IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
        ideaModel.getProject().getIpr().withXml(xmlProvider -> {
            // this block is lazy
            List<URI> uris = implConfiguration.getFiles().stream().map(File::toURI).collect(Collectors.toList());
            ConfigureJavaFormatterXml.configureJavaFormat(xmlProvider.asNode(), uris);
            ConfigureJavaFormatterXml.configureExternalDependencies(xmlProvider.asNode());
        });
    }

    private static void configureIntelliJImport(Project project, Configuration implConfiguration) {
        project.getPluginManager().apply("org.jetbrains.gradle.plugin.idea-ext");

        Provider<? extends Task> configurePalantirJavaFormatXmlTask = project.getTasks()
                .register("configurePalantirJavaFormatXml", ConfigurePalantirJavaFormatXml.class, task -> {
                    task.getImplConfiguration().set(implConfiguration);
                });

        Provider<? extends Task> configurePalantirJavaFormatPluginDependencyXml = project.getTasks()
                .register("configurePalantirJavaFormatPluginDependencyXml", ConfigureExternalDependenciesXml.class);

        Task palantirJavaFormatIntellij = project.getTasks().create("palantirJavaFormatIntellij", task -> {
            task.setDescription("Configure IntelliJ directory-based repository after importing");
            task.setGroup(UpdateIntellijXmlTask.INTELLIJ_TASK_GROUP);
            task.dependsOn(configurePalantirJavaFormatXmlTask, configurePalantirJavaFormatPluginDependencyXml);
        });

        ExtensionAware ideaProject = (ExtensionAware) project.getExtensions().getByType(IdeaModel.class).getProject();
        ExtensionAware settings = (ExtensionAware) ideaProject.getExtensions().getByName("settings");
        TaskTriggersConfig taskTriggers = settings.getExtensions().getByType(TaskTriggersConfig.class);
        taskTriggers.beforeSync(palantirJavaFormatIntellij);
    }
}
