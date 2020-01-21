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
import com.google.common.io.Files;
import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.xml.sax.SAXException;

public final class PalantirJavaFormatIdeaPlugin implements Plugin<Project> {

    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-idea to the root project");

        rootProject.getPlugins().apply(PalantirJavaFormatProviderPlugin.class);

        rootProject.getPluginManager().withPlugin("idea", ideaPlugin -> {
            Configuration implConfiguration =
                    rootProject.getConfigurations().getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME);

            configureLegacyIdea(rootProject, implConfiguration);
            configureIntelliJImport(rootProject, implConfiguration);
        });
    }

    private static void configureLegacyIdea(Project project, Configuration implConfiguration) {
        IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
        ideaModel.getProject().getIpr().withXml(xmlProvider -> {
            // this block is lazy
            List<URI> uris =
                    implConfiguration.getFiles().stream().map(File::toURI).collect(Collectors.toList());
            ConfigureJavaFormatterXml.configureJavaFormat(xmlProvider.asNode(), uris);
            ConfigureJavaFormatterXml.configureExternalDependencies(xmlProvider.asNode());
        });
    }

    private static void configureIntelliJImport(Project project, Configuration implConfiguration) {
        // Note: we tried using 'org.jetbrains.gradle.plugin.idea-ext' and afterSync triggers, but these are currently
        // very hard to manage as the tasks feel disconnected from the Sync operation, and you can't remove them once
        // you've added them. For that reason, we accept that we have to resolve this configuration at
        // configuration-time, but only do it when part of an IDEA import.
        if (!Boolean.getBoolean("idea.active")) {
            return;
        }
        project.getGradle().projectsEvaluated(gradle -> {
            List<URI> uris =
                    implConfiguration.getFiles().stream().map(File::toURI).collect(Collectors.toList());

            createOrUpdateIdeaXmlFile(project.file(".idea/palantir-java-format.xml"), node ->
                    ConfigureJavaFormatterXml.configureJavaFormat(node, uris));
            createOrUpdateIdeaXmlFile(project.file(".idea/externalDependencies.xml"), node ->
                    ConfigureJavaFormatterXml.configureExternalDependencies(node));
        });
    }

    private static void createOrUpdateIdeaXmlFile(File configurationFile, Consumer<Node> configure) {
        Node rootNode;
        if (configurationFile.isFile()) {
            try {
                rootNode = new XmlParser().parse(configurationFile);
            } catch (IOException | SAXException | ParserConfigurationException e) {
                throw new RuntimeException("Couldn't parse existing configuration file: " + configurationFile, e);
            }
        } else {
            rootNode = new Node(null, "project", ImmutableMap.of("version", "4"));
        }

        configure.accept(rootNode);

        try (BufferedWriter writer = Files.newWriter(configurationFile, Charset.defaultCharset());
                PrintWriter printWriter = new PrintWriter(writer)) {
            XmlNodePrinter nodePrinter = new XmlNodePrinter(printWriter);
            nodePrinter.setPreserveWhitespace(true);
            nodePrinter.print(rootNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write back to configuration file: " + configurationFile, e);
        }
    }
}
