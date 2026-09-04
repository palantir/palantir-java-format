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
import java.util.Map;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;

public abstract class NativeImageFormatProviderPlugin implements Plugin<Project> {

    @Nested
    protected abstract GradleOperatingSystem getOs();

    static final String NATIVE_CONFIGURATION_NAME = "palantirJavaFormatNative";
    static final String NATIVE_RESOLVABLE_CONFIGURATION_NAME = "palantirJavaFormatNativeClasspath";
    private static final String NATIVE_DEPENDENCIES_CONFIGURATION_NAME =
            "palantirJavaFormatNativeClasspathDependencies";
    private static final String NATIVE_ELEMENTS_CONFIGURATION_NAME = "palantirJavaFormatNativeElements";
    private static final String NATIVE_CAPABILITY = "com.palantir.javaformat:palantir-java-format-native-provider:1";

    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-provider to the root project");

        Provider<OperatingSystem> operatingSystem = getOs().getOperatingSystem();
        String implementationVersion = JavaFormatExtension.class.getPackage().getImplementationVersion();
        NamedDomainObjectProvider<DependencyScopeConfiguration> dependencies = rootProject
                .getConfigurations()
                .dependencyScope(NATIVE_CONFIGURATION_NAME, configuration -> {
                    configuration.setDescription("Declares the palantir-java-format native image dependencies");
                    configuration.defaultDependencies(defaultDependencies -> defaultDependencies.addAllLater(
                            operatingSystem.map(os -> Collections.singletonList(rootProject
                                    .getDependencies()
                                    .create(String.format(
                                            "com.palantir.javaformat:palantir-java-format-native:%s:nativeImage-%s_%s@%s",
                                            implementationVersion,
                                            os.uiName(),
                                            Architecture.get().uiName(),
                                            getExtension(os)))))));
                });
        rootProject.getConfigurations().consumable(NATIVE_ELEMENTS_CONFIGURATION_NAME, configuration -> {
            configuration.setDescription("Provides the palantir-java-format native image to other projects");
            configuration.extendsFrom(dependencies.get());
            configuration.getOutgoing().capability(NATIVE_CAPABILITY);
            configuration
                    .getAttributes()
                    .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        });

        getNativeImageConfiguration(rootProject, operatingSystem);
    }

    static Configuration getNativeImageConfiguration(Project project, Provider<OperatingSystem> operatingSystem) {
        Configuration existing = project.getConfigurations().findByName(NATIVE_RESOLVABLE_CONFIGURATION_NAME);
        if (existing != null) {
            return existing;
        }

        NamedDomainObjectProvider<? extends Configuration> dependencies;
        if (project == project.getRootProject()) {
            dependencies =
                    project.getConfigurations().named(NATIVE_CONFIGURATION_NAME, DependencyScopeConfiguration.class);
        } else {
            dependencies = project.getConfigurations()
                    .dependencyScope(NATIVE_DEPENDENCIES_CONFIGURATION_NAME, configuration -> {
                        configuration.setDescription("Selects the root palantir-java-format native image");
                        ProjectDependency rootProvider = (ProjectDependency) project.getDependencies()
                                .project(Map.of("path", project.getRootProject().getPath()));
                        rootProvider.capabilities(capabilities -> capabilities.requireCapability(NATIVE_CAPABILITY));
                        configuration.getDependencies().add(rootProvider);
                    });
        }

        Configuration configuration = createResolvableConfiguration(project, dependencies);
        registerTransform(project, operatingSystem);
        return configuration;
    }

    private static Configuration createResolvableConfiguration(
            Project project, NamedDomainObjectProvider<? extends Configuration> dependencies) {
        NamedDomainObjectProvider<ResolvableConfiguration> configuration = project.getConfigurations()
                .resolvable(NATIVE_RESOLVABLE_CONFIGURATION_NAME, resolvable -> {
                    resolvable.setDescription("Resolves the palantir-java-format native image for this project");
                    resolvable.extendsFrom(dependencies.get());
                    resolvable
                            .getAttributes()
                            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
                });
        return configuration.get();
    }

    private static void registerTransform(Project project, Provider<OperatingSystem> operatingSystem) {
        project.getDependencies().registerTransform(ExecutableTransform.class, transformSpec -> {
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
