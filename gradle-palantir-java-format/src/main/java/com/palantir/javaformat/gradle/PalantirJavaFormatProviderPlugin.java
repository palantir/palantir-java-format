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

import com.google.common.base.Preconditions;
import java.util.Map;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.attributes.Attribute;

public final class PalantirJavaFormatProviderPlugin implements Plugin<Project> {

    static final String CONFIGURATION_NAME = "palantirJavaFormat";
    static final String RESOLVABLE_CONFIGURATION_NAME = "palantirJavaFormatClasspath";
    private static final String DEPENDENCIES_CONFIGURATION_NAME = "palantirJavaFormatClasspathDependencies";
    private static final String ELEMENTS_CONFIGURATION_NAME = "palantirJavaFormatElements";
    private static final String CAPABILITY = "com.palantir.javaformat:palantir-java-format-provider:1";
    private static final Attribute<String> PROVIDER_ATTRIBUTE =
            Attribute.of("com.palantir.javaformat.provider", String.class);

    @SuppressWarnings({"for-rollout:GradleTypesAsFields", "for-rollout:NonAbstractGradleType"})
    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-provider to the root project");

        NamedDomainObjectProvider<DependencyScopeConfiguration> dependencies = rootProject
                .getConfigurations()
                .dependencyScope(CONFIGURATION_NAME, configuration -> {
                    configuration.setDescription("Declares the palantir-java-format implementation dependencies");
                    configuration.defaultDependencies(defaultDependencies -> defaultDependencies.add(rootProject
                            .getDependencies()
                            .create(String.format(
                                    "com.palantir.javaformat:palantir-java-format:%s",
                                    JavaFormatExtension.class.getPackage().getImplementationVersion()))));
                });

        rootProject.getConfigurations().consumable(ELEMENTS_CONFIGURATION_NAME, configuration -> {
            configuration.setDescription("Provides the palantir-java-format implementation to other projects");
            configuration.extendsFrom(dependencies.get());
            configuration.getOutgoing().capability(CAPABILITY);
            configuration.getAttributes().attribute(PROVIDER_ATTRIBUTE, "java");
        });

        rootProject.getPluginManager().apply(NativeImageFormatProviderPlugin.class);

        Configuration configuration = createResolvableConfiguration(rootProject, dependencies);
        rootProject.getExtensions().create("palantirJavaFormat", JavaFormatExtension.class, configuration);
    }

    static Configuration getImplementationConfiguration(Project project) {
        Configuration existing = project.getConfigurations().findByName(RESOLVABLE_CONFIGURATION_NAME);
        if (existing != null) {
            return existing;
        }

        if (project == project.getRootProject()) {
            return createResolvableConfiguration(
                    project, project.getConfigurations().named(CONFIGURATION_NAME, DependencyScopeConfiguration.class));
        }

        NamedDomainObjectProvider<DependencyScopeConfiguration> dependencies = project.getConfigurations()
                .dependencyScope(DEPENDENCIES_CONFIGURATION_NAME, configuration -> {
                    configuration.setDescription("Selects the root palantir-java-format implementation");
                    ProjectDependency rootProvider = (ProjectDependency) project.getDependencies()
                            .project(Map.of("path", project.getRootProject().getPath()));
                    rootProvider.capabilities(capabilities -> capabilities.requireCapability(CAPABILITY));
                    configuration.getDependencies().add(rootProvider);
                });
        return createResolvableConfiguration(project, dependencies);
    }

    private static Configuration createResolvableConfiguration(
            Project project, NamedDomainObjectProvider<? extends Configuration> dependencies) {
        NamedDomainObjectProvider<ResolvableConfiguration> configuration = project.getConfigurations()
                .resolvable(RESOLVABLE_CONFIGURATION_NAME, resolvable -> {
                    resolvable.setDescription("Resolves the palantir-java-format implementation for this project");
                    resolvable.extendsFrom(dependencies.get());
                    resolvable.getAttributes().attribute(PROVIDER_ATTRIBUTE, "java");
                });
        return configuration.get();
    }
}
