package com.palantir.javaformat.gradle;

import groovy.util.Node;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;

public class ConfigurePalantirJavaFormatXml extends UpdateIntellijXmlTask {
    private final Property<Configuration> implConfiguration = getProject().getObjects().property(Configuration.class);

    @Classpath
    Property<Configuration> getImplConfiguration() {
        return implConfiguration;
    }

    public ConfigurePalantirJavaFormatXml() {
        getXmlFile().set(getProject().file(".idea/palantir-java-format.xml"));
    }

    @Override
    protected final void configure(Node rootNode) {
        List<URI> uris = implConfiguration.get().getFiles().stream().map(File::toURI).collect(Collectors.toList());
        ConfigureJavaFormatterXml.configureJavaFormat(rootNode, uris);
    }
}
