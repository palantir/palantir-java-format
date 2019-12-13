package com.palantir.javaformat.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PalantirJavaFormatSpotlessPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getRootProject().getPluginManager().apply(PalantirJavaFormatProviderPlugin.class);

        project.getPluginManager().withPlugin("java", plugin -> {
            project.getPluginManager().withPlugin("com.diffplug.gradle.spotless", spotlessPlugin -> {
                SpotlessInterop.addSpotlessJavaStep(project, PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME);
            });
        });
    }
}
