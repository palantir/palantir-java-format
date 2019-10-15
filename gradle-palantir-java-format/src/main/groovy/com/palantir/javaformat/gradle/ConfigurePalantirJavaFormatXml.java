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
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.xml.sax.SAXException;

public class ConfigurePalantirJavaFormatXml extends DefaultTask {
    private final Property<Configuration> implConfiguration = getProject().getObjects().property(Configuration.class);

    @Classpath
    Property<Configuration> getImplConfiguration() {
        return implConfiguration;
    }

    @PathSensitive(PathSensitivity.RELATIVE)
    @OutputFile
    File getOutputFile() {
        return getProject().file(".idea/palantir-java-format.xml");
    }

    @TaskAction
    public void run() {
        List<URI> uris = implConfiguration.get().getFiles().stream().map(File::toURI).collect(Collectors.toList());
        File configurationFile = getOutputFile();
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
        ConfigureJavaFormatterXml.configure(rootNode, Optional.of(uris));
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
