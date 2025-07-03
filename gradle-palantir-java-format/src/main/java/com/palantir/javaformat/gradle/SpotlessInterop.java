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

import com.diffplug.gradle.spotless.JavaExtension;
import com.diffplug.spotless.FormatterStep;
import com.palantir.javaformat.gradle.spotless.NativePalantirJavaFormatStep;
import com.palantir.javaformat.gradle.spotless.PalantirJavaFormatStep;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Nested;

/**
 * Class that exists only to encapsulate accessing spotless classes, so that Gradle can generate a decorated class for
 * {@link com.palantir.javaformat.gradle.PalantirJavaFormatSpotlessPlugin} even if spotless is not on the classpath.
 */
public abstract class SpotlessInterop implements Action<JavaExtension> {
    private static final Logger logger = Logging.getLogger(SpotlessInterop.class);

    private final Project project;

    @Nested
    protected abstract NativeImageSupport getNativeImageConfigured();

    @Inject
    protected abstract ConfigurationContainer getConfigurations();

    @Inject
    public SpotlessInterop(Project project) {
        this.project = project;
    }

    @Override
    public void execute(JavaExtension java) {
        // This is configuration cache safe as happening afterEvaluate
        java.addStep(spotlessJavaFormatStep());
    }

    private FormatterStep spotlessJavaFormatStep() {
        if (getNativeImageConfigured().isNativeImageConfigured()
                && JavaVersion.current().compareTo(JavaVersion.VERSION_21) < 0) {
            logger.info("Using the native-image formatter");
            return NativePalantirJavaFormatStep.create(
                    getConfigurations().getByName(NativeImageFormatProviderPlugin.NATIVE_CONFIGURATION_NAME));
        }
        logger.info("Using the Java-based formatter {}", JavaVersion.current());
        return PalantirJavaFormatStep.create(
                getConfigurations().getByName(PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME),
                project.getRootProject().getExtensions().getByType(JavaFormatExtension.class));
    }
}
