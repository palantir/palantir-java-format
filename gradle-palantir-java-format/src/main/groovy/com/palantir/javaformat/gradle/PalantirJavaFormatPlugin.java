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

import com.palantir.javaformat.bootstrap.NativeImageFormatterService;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class PalantirJavaFormatPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getRootProject().getPlugins().apply(PalantirJavaFormatProviderPlugin.class);
        project.getRootProject().getPlugins().apply(NativeImageFormatProviderPlugin.class);
        project.getRootProject().getPlugins().apply(PalantirJavaFormatIdeaPlugin.class);

        project.getPlugins().apply(PalantirJavaFormatSpotlessPlugin.class);

        project.getPlugins().withId("java", p -> {

            // TODO(dfox): in the future we may want to offer a simple 'format' task so people don't need to use
            // spotless to try out our formatter
            project.getTasks().register("formatDiff", FormatDiffTask.class, task -> {
                if (NativeImageFormatProviderPlugin.isNativeImageConfigured(project)) {
                    System.err.println("Configuring native");
                    task.getNativeImage().fileProvider(getNativeImplConfiguration(project));
                }
            });

            project.getGradle()
                    .getSharedServices()
                    .registerIfAbsent("formatterService", FormatterServiceService.class, spec -> {
                        spec.getParameters().getExtension().set(project.provider(() -> project.getExtensions()
                                .getByType(JavaFormatExtension.class)));
                    });
        });
    }

    private static Provider<File> getNativeImplConfiguration(Project project) {
        return (project.getRootProject()
                .getConfigurations()
                .named(NativeImageFormatProviderPlugin.NATIVE_CONFIGURATION_NAME)
                .map(FileCollection::getSingleFile));
    }

    public abstract static class FormatDiffTask extends DefaultTask {
        @Inject
        protected abstract ProjectLayout getProjectLayout();

        private static Logger log = Logging.getLogger(FormatDiffTask.class);

        @org.gradle.api.tasks.Optional
        @InputFile
        abstract RegularFileProperty getNativeImage();

        @ServiceReference("formatterService")
        abstract Property<FormatterServiceService> getFormatterServiceService();

        public FormatDiffTask() {
            setDescription("Format only chunks of files that appear in git diff");
            setGroup("Formatting");
        }

        @TaskAction
        public final void formatDiff() throws IOException, InterruptedException {
            if (getNativeImage().isPresent()) {
                log.info("Using the native-image formatter");
                FormatDiff.formatDiff(
                        getProjectLayout().getProjectDirectory().getAsFile().toPath(),
                        new NativeImageFormatterService(
                                getNativeImage().get().getAsFile().toPath()));
            } else {
                log.info("Using the Java-based formatter");
                FormatDiff.formatDiff(
                        getProjectLayout().getProjectDirectory().getAsFile().toPath(),
                        getFormatterServiceService().get().getFormatterService());
            }
        }
    }
}
