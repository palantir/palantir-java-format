/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.javaformat.gradle;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.spotless.FormatterStep;
import com.google.common.io.Resources;
import com.palantir.javaformat.gradle.spotless.NativePalantirJavaFormatStep;
import com.palantir.javaformat.gradle.spotless.PalantirJavaFormatStep;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import org.gradle.api.Project;

/**
 * Class that exists only to encapsulate accessing spotless classes, so that Gradle can generate a decorated class for
 * {@link com.palantir.javaformat.gradle.PalantirJavaFormatSpotlessPlugin} even if spotless is not on the classpath.
 */
final class SpotlessInterop {
    private SpotlessInterop() {}

    static void addSpotlessJavaStep(Project project, String configurationName) {
        SpotlessExtension spotlessExtension = project.getExtensions().getByType(SpotlessExtension.class);
        spotlessExtension.java(java -> java.addStep(addSpotlessJavaFormatStep(project, configurationName)));
    }

    static FormatterStep addSpotlessJavaFormatStep(Project project, String configurationName) {
        Boolean legacyFormatter = Optional.ofNullable(project.findProperty("palantir.legacy.formatter"))
                .map(value -> Boolean.getBoolean((String) value))
                .orElse(false);
        if (legacyFormatter) {
            return PalantirJavaFormatStep.create(
                    project.getRootProject().getConfigurations().getByName(configurationName),
                    project.getRootProject().getExtensions().getByType(JavaFormatExtension.class));
        } else {
            try {
                URL resourceUrl = Resources.getResource("palantir-java-format");
                return NativePalantirJavaFormatStep.create(
                        Path.of(resourceUrl.toURI().getPath()).toFile());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Palantir java format native image was not found", e);
            }
        }
    }
}
