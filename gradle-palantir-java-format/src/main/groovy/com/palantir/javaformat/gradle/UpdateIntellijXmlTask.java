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
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.xml.sax.SAXException;

public abstract class UpdateIntellijXmlTask extends DefaultTask {
    private final Property<File> outputFile = getProject().getObjects().property(File.class);

    @PathSensitive(PathSensitivity.RELATIVE)
    @OutputFile
    protected final Property<File> getOutputFile() {
        return outputFile;
    }

    @TaskAction
    public final void run() {
        File configurationFile = getOutputFile().get();
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

    protected abstract void configure(Node rootNode);
}
