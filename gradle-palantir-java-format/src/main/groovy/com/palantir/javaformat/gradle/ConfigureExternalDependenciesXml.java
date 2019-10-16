package com.palantir.javaformat.gradle;

import groovy.util.Node;

public class ConfigureExternalDependenciesXml extends UpdateIntellijXmlTask {
    public ConfigureExternalDependenciesXml() {
        getOutputFile().set(getProject().file(".idea/externalDependencies.xml"));
    }

    @Override
    protected void configure(Node rootNode) {
        ConfigureJavaFormatterXml.configureExternalDependencies(rootNode);
    }
}
