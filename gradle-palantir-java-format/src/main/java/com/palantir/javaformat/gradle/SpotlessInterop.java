package com.palantir.javaformat.gradle;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.palantir.javaformat.gradle.spotless.PalantirJavaFormatStep;
import org.gradle.api.Project;

/**
 * Class that exists only to encapsulate accessing spotless classes, so that Gradle can generate a decorated class for
 * {@link com.palantir.javaformat.gradle.PalantirJavaFormatSpotlessPlugin} even if spotless is not on the classpath.
 */
final class SpotlessInterop {
    private SpotlessInterop() {}

    static void addSpotlessJavaStep(Project project, String configurationName) {
        SpotlessExtension spotlessExtension = project.getExtensions().getByType(SpotlessExtension.class);
        spotlessExtension.java(java -> java.addStep(PalantirJavaFormatStep.create(
                project.getRootProject().getConfigurations().getByName(configurationName),
                project.getRootProject().getExtensions().getByType(JavaFormatExtension.class))));
    }
}
