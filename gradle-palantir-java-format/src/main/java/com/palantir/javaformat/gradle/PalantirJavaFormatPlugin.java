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

import com.google.common.collect.Iterables;
import com.palantir.javaformat.java.FormatterService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public final class PalantirJavaFormatPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getRootProject().getPlugins().apply(PalantirJavaFormatProviderPlugin.class);
        project.getPlugins().withId("java", p -> {
            project.getTasks().register("formatDiff", FormatDiffTask.class);

            // TODO(dfox): in the future we may want to offer a simple 'format' task so people don't need to use
            // spotless to try out our formatter
        });
    }

    public static class FormatDiffTask extends DefaultTask {
        public FormatDiffTask() {
            setDescription("Format only chunks of files that appear in git diff");
            setGroup("Formatting");
        }

        @TaskAction
        public final void formatDiff() throws IOException, InterruptedException {
            URL[] jarUris = getProject()
                    .getRootProject()
                    .getConfigurations()
                    .getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME)
                    .getFiles()
                    .stream()
                    .map(file -> {
                        try {
                            return file.toURI().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException("Unable to convert URI to URL: " + file, e);
                        }
                    })
                    .toArray(URL[]::new);

            ClassLoader classLoader = new URLClassLoader(jarUris, PalantirJavaFormatPlugin.class.getClassLoader());
            FormatterService formatter =
                    Iterables.getOnlyElement(ServiceLoader.load(FormatterService.class, classLoader));

            FormatDiff.formatDiff(getProject().getProjectDir().toPath(), formatter);
        }
    }
}
