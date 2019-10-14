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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.plugins.ide.idea.model.IdeaModel;

/**
 * Plugin to configure the PalantirJavaFormat IDEA plugin based on an optional implementation version of the formatter.
 */
public class JavaFormatPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "palantirJavaFormat";

    @Override
    public void apply(Project project) {
        JavaFormatExtension extension =
                project.getExtensions().create(EXTENSION_NAME, JavaFormatExtension.class, project);

        Configuration implConfiguration = project.getConfigurations().create("palantirJavaFormat", conf -> {
            conf.setDescription("Internal configuration for resolving the palantirJavaFormat implementation");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
            conf.getDependencies().addLater(project.provider(() -> {
                // It's fine if this is null, as we never resolve the configuration in that case.
                String version = extension.getImplementationVersion().getOrNull();
                return project.getDependencies().create(ImmutableMap.of(
                        "group", "com.palantir.javaformat",
                        "name", "palantir-java-format",
                        "version", version));
            }));
        });

        project.getPluginManager().withPlugin("idea", ideaPlugin -> {
            IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
            ideaModel.getProject().getIpr().withXml(xmlProvider -> {
                // this block is lazy
                Optional<List<URI>> uris = Optional.ofNullable(
                        extension
                                .getImplementationVersion()
                                .map(version -> implConfiguration.getFiles().stream().map(File::toURI).collect(
                                        Collectors.toList()))
                                .getOrNull());
                ConfigureJavaFormatterXml.configure(xmlProvider.asNode(), uris);
            });
        });
    }
}
