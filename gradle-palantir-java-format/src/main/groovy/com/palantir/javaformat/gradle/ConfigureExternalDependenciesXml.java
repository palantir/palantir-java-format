package com.palantir.javaformat.gradle;

import groovy.util.Node;

public class ConfigureExternalDependenciesXml extends UpdateIntellijXmlTask {
    public ConfigureExternalDependenciesXml() {
        getXmlFile().set(getProject().file(".idea/externalDependencies.xml"));
    }

    @Override
    protected final void configure(Node rootNode) {
        ConfigureJavaFormatterXml.configureExternalDependencies(rootNode);
    }
}
