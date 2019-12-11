package com.palantir.javaformat.gradle;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.palantir.javaformat.gradle.spotless.PalantirJavaFormatStep;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PalantirJavaFormatSpotlessPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getRootProject().getPluginManager().apply(PalantirJavaFormatProviderPlugin.class);

        project.getPluginManager().withPlugin("java", plugin -> {
            project.getPluginManager().withPlugin("com.diffplug.gradle.spotless", spotlessPlugin -> {
                SpotlessExtension spotlessExtension = project.getExtensions().getByType(SpotlessExtension.class);
                spotlessExtension.java(java -> java.addStep(PalantirJavaFormatStep.create(
                        project.getRootProject()
                                .getConfigurations()
                                .getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME),
                        project.getRootProject().getExtensions().getByType(JavaFormatExtension.class))));
            });
        });
    }
}
