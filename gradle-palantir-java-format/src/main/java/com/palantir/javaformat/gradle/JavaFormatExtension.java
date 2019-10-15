package com.palantir.javaformat.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class JavaFormatExtension {
    private final Property<String> implementationVersion;
    private static final String IMPLEMENTATION_VERSION =
            JavaFormatExtension.class.getPackage().getImplementationVersion();

    public JavaFormatExtension(Project project) {
        implementationVersion = project.getObjects().property(String.class);
        implementationVersion.set(IMPLEMENTATION_VERSION);
    }

    public final Property<String> getImplementationVersion() {
        return implementationVersion;
    }
}
