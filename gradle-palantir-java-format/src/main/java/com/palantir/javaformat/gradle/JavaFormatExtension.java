package com.palantir.javaformat.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class JavaFormatExtension {
    private final Property<String> implementationVersion;

    public JavaFormatExtension(Project project) {
        implementationVersion = project.getObjects().property(String.class);
    }

    public final Property<String> getImplementationVersion() {
        return implementationVersion;
    }
}
