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
import com.palantir.javaformat.gradle.spotless.NativePalantirJavaFormatStep;
import com.palantir.javaformat.gradle.spotless.PalantirJavaFormatStep;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Class that exists only to encapsulate accessing spotless classes, so that Gradle can generate a decorated class for
 * {@link com.palantir.javaformat.gradle.PalantirJavaFormatSpotlessPlugin} even if spotless is not on the classpath.
 */
final class SpotlessInterop {
    private static Logger logger = Logging.getLogger(SpotlessInterop.class);

    private SpotlessInterop() {}

    static void addSpotlessJavaStep(Project project) {
        SpotlessExtension spotlessExtension = project.getExtensions().getByType(SpotlessExtension.class);
        spotlessExtension.java(java -> java.addStep(addSpotlessJavaFormatStep(project)));
    }

    static FormatterStep addSpotlessJavaFormatStep(Project project) {
        if (NativeImageFormatProviderPlugin.shouldUseNativeImage(project)) {
            logger.info("Using the native-image palantir-java-formatter");
            return NativePalantirJavaFormatStep.create(project.getRootProject()
                    .getConfigurations()
                    .getByName(NativeImageFormatProviderPlugin.NATIVE_CONFIGURATION_NAME));
        }
        logger.info("Using the legacy palantir-java-formatter");
        return PalantirJavaFormatStep.create(
                project.getRootProject()
                        .getConfigurations()
                        .getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME),
                project.getRootProject().getExtensions().getByType(JavaFormatExtension.class));
    }
}
