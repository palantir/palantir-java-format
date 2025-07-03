/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.base.Preconditions;
import com.palantir.platform.Architecture;
import com.palantir.platform.GradleOperatingSystem;
import com.palantir.platform.OperatingSystem;
import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;

public abstract class NativeImageFormatProviderPlugin implements Plugin<Project> {

    @Nested
    protected abstract GradleOperatingSystem getOs();

    static final String NATIVE_CONFIGURATION_NAME = "palantirJavaFormatNative";

    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-provider to the root project");

        Provider<OperatingSystem> operatingSystem = getOs().getOperatingSystem();
        String implementationVersion = JavaFormatExtension.class.getPackage().getImplementationVersion();
        rootProject.getConfigurations().register(NATIVE_CONFIGURATION_NAME, conf -> {
            conf.setDescription("Internal configuration for resolving the palantir-java-format native image");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);
            conf.defaultDependencies(deps -> {
                deps.addAllLater(operatingSystem.map(os -> Collections.singletonList(rootProject
                        .getDependencies()
                        .create(String.format(
                                "com.palantir.javaformat:palantir-java-format-native:%s:nativeImage-%s_%s@%s",
                                implementationVersion,
                                os.uiName(),
                                Architecture.get().uiName(),
                                getExtension(os))))));
            });
            conf.getAttributes().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        });
        rootProject.getDependencies().registerTransform(ExecutableTransform.class, transformSpec -> {
            transformSpec
                    .getFrom()
                    .attributeProvider(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                            operatingSystem.map(NativeImageFormatProviderPlugin::getExtension));
            transformSpec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        });
    }

    static String getExtension(OperatingSystem operatingSystem) {
        if (operatingSystem.equals(OperatingSystem.WINDOWS)) {
            return "exe";
        }
        return "bin";
    }
}
