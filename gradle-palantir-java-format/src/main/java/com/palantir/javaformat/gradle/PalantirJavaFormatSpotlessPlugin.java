/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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
