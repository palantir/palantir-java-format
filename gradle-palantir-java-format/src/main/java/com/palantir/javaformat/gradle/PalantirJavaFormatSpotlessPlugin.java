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

import com.google.common.collect.ImmutableList;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PalantirJavaFormatSpotlessPlugin implements Plugin<Project> {
    // The spotless gradle plugin got renamed to 'com.diffplug.spotless' at version 5.0.0
    private static final ImmutableList<String> SPOTLESS_PLUGINS =
            ImmutableList.of("com.diffplug.gradle.spotless", "com.diffplug.spotless");

    @Override
    public void apply(Project project) {
        project.getRootProject().getPluginManager().apply(PalantirJavaFormatProviderPlugin.class);

        project.getPluginManager().withPlugin("java", _javaPlugin -> {
            SPOTLESS_PLUGINS.forEach(
                    spotlessPluginId -> project.getPluginManager().withPlugin(spotlessPluginId, _spotlessPlugin -> {
                        SpotlessInterop.addSpotlessJavaStep(
                                project, PalantirJavaFormatProviderPlugin.CONFIGURATION_NAME);
                    }));
        });
    }
}
