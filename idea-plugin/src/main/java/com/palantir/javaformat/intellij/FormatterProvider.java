/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat.intellij;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.palantir.javaformat.bootstrap.BootstrappingFormatterService;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.logsafe.Preconditions;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.Attributes.Name;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FormatterProvider {
    private static final Logger log = LoggerFactory.getLogger(FormatterProvider.class);

    // Cache to avoid creating a URLClassloader every time we want to format from IntelliJ
    private final LoadingCache<FormatterCacheKey, FormatterService> implementationCache =
            Caffeine.newBuilder().maximumSize(1).build(FormatterProvider::createFormatter);

    FormatterService get(Project project, PalantirJavaFormatSettings settings) {
        return implementationCache.get(new FormatterCacheKey(
                project, settings.getImplementationClassPath(), settings.injectedVersionIsOutdated()));
    }

    private static FormatterService createFormatter(FormatterCacheKey cacheKey) {
        List<Path> implementationClasspath =
                getImplementationUrls(cacheKey.implementationClassPath, cacheKey.useBundledImplementation);
        Path jdkPath = getJdkPath(cacheKey.project);
        Integer jdkMajorVersion = getSdkVersion(cacheKey.project);

        // Just enable the bootstrapping formatter for projects using Java 16+
        // TODO(fwindheuser): Enable for all
        if (jdkMajorVersion >= 16) {
            log.debug("Running formatter with jdk version {} and path: {}", jdkMajorVersion, jdkPath);
            return new BootstrappingFormatterService(jdkPath, jdkMajorVersion, implementationClasspath);
        }

        // Use "in-process" formatter service
        URL[] implementationUrls = toUrlsUnchecked(implementationClasspath);
        ClassLoader classLoader = new URLClassLoader(implementationUrls, FormatterService.class.getClassLoader());
        return Iterables.getOnlyElement(ServiceLoader.load(FormatterService.class, classLoader));
    }

    private static List<Path> getProvidedImplementationUrls(List<URI> implementationClasspath) {
        return implementationClasspath.stream().map(Path::of).collect(Collectors.toList());
    }

    private static List<Path> getBundledImplementationUrls() {
        // Load from the jars bundled with the plugin.
        Path implDir = PalantirCodeStyleManager.PLUGIN.getPath().toPath().resolve("impl");
        log.debug("Using palantir-java-format implementation bundled with plugin: {}", implDir);
        return listDirAsUrlsUnchecked(implDir);
    }

    private static List<Path> getImplementationUrls(
            Optional<List<URI>> implementationClassPath, boolean useBundledImplementation) {
        if (useBundledImplementation) {
            log.debug("Using palantir-java-format implementation bundled with plugin");
            return getBundledImplementationUrls();
        }
        return implementationClassPath
                .map(classpath -> {
                    log.debug("Using palantir-java-format implementation defined by URIs: {}", classpath);
                    return getProvidedImplementationUrls(classpath);
                })
                .orElseGet(() -> {
                    log.debug("Using palantir-java-format implementation bundled with plugin");
                    return getBundledImplementationUrls();
                });
    }

    private static Path getJdkPath(Project project) {
        return getProjectJdk(project)
                .map(Sdk::getHomePath)
                .map(Path::of)
                .map(sdkHome -> sdkHome.resolve("bin").resolve("java"))
                .filter(Files::exists)
                .orElseThrow(() -> new IllegalStateException("Could not determine jdk path for project " + project));
    }

    private static Integer getSdkVersion(Project project) {
        return getProjectJdk(project)
                .map(FormatterProvider::parseSdkJavaVersion)
                .orElseThrow(() -> new IllegalStateException("Could not determine jdk version for project " + project));
    }

    private static Integer parseSdkJavaVersion(Sdk sdk) {
        // Parses the actual version out of "SDK#getVersionString" which returns 'java version "15"'.
        String version = Preconditions.checkNotNull(
                JdkUtil.getJdkMainAttribute(sdk, Name.IMPLEMENTATION_VERSION), "JDK version is null");
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            log.error("Could not parse sdk version: {}", version, e);
            return null;
        }
    }

    private static Optional<Sdk> getProjectJdk(Project project) {
        return Optional.ofNullable(ProjectRootManager.getInstance(project).getProjectSdk());
    }

    private static URL[] toUrlsUnchecked(List<Path> paths) {
        return paths.stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (IllegalArgumentException | MalformedURLException e) {
                        throw new RuntimeException("Couldn't convert Path to URL: " + path, e);
                    }
                })
                .toArray(URL[]::new);
    }

    private static List<Path> listDirAsUrlsUnchecked(Path dir) {
        try (Stream<Path> list = Files.list(dir)) {
            return list.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't list dir: " + dir, e);
        }
    }

    private static final class FormatterCacheKey {
        private final Project project;
        private final Optional<List<URI>> implementationClassPath;
        private final boolean useBundledImplementation;

        FormatterCacheKey(
                Project project, Optional<List<URI>> implementationClassPath, boolean useBundledImplementation) {
            this.project = project;
            this.implementationClassPath = implementationClassPath;
            this.useBundledImplementation = useBundledImplementation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FormatterCacheKey that = (FormatterCacheKey) o;
            return useBundledImplementation == that.useBundledImplementation
                    && project.equals(that.project)
                    && implementationClassPath.equals(that.implementationClassPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, implementationClassPath, useBundledImplementation);
        }
    }
}
