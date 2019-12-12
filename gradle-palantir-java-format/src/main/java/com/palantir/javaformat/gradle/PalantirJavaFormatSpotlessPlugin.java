package com.palantir.javaformat.gradle;

import java.lang.reflect.InvocationTargetException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PalantirJavaFormatSpotlessPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getRootProject().getPluginManager().apply(PalantirJavaFormatProviderPlugin.class);

        project.getPluginManager().withPlugin("java", plugin -> {
            project.getPluginManager().withPlugin("com.diffplug.gradle.spotless", spotlessPlugin -> {
                try {
                    project.getBuildscript()
                            .getClassLoader()
                            .loadClass("com.palantir.javaformat.gradle.SpotlessInterop")
                            .getDeclaredMethod("addSpotlessJavaStep", Project.class, String.class)
                            .invoke(null, project, PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME);
                } catch (IllegalAccessException
                        | InvocationTargetException
                        | NoSuchMethodException
                        | ClassNotFoundException e) {
                    throw new RuntimeException("Failed to do magic", e);
                }
            });
        });
    }
}
