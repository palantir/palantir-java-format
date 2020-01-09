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
import com.google.common.io.Files;
import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.xml.sax.SAXException;

public abstract class UpdateIntellijXmlTask extends DefaultTask {
    private final Property<File> xmlFile = getProject().getObjects().property(File.class);
    static final String INTELLIJ_TASK_GROUP = "intellij";

    protected abstract void configure(Node rootNode);

    public UpdateIntellijXmlTask() {
        setGroup(INTELLIJ_TASK_GROUP);
    }

    @OutputFile
    protected final Property<File> getXmlFile() {
        return xmlFile;
    }

    @TaskAction
    public final void run() {
        File configurationFile = getXmlFile().get();
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

        configure(rootNode);

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
