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

import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.palantir.javaformat.bootstrap.BootstrappingFormatterService;
import com.palantir.javaformat.java.FormatterService;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.internal.jvm.Jvm;

public class JavaFormatExtension {
    private final Project project;
    private final Configuration configuration;
    private final Supplier<FormatterService> memoizedService;

    public JavaFormatExtension(Project project, Configuration configuration) {
        this.project = project;
        this.configuration = configuration;
        this.memoizedService = Suppliers.memoize(this::loadFormatterServiceInternal);
    }

    public FormatterService loadFormatterService() {
        return memoizedService.get();
    }

    private FormatterService loadFormatterServiceInternal() {
        if (JavaVersion.current().compareTo(JavaVersion.VERSION_15) > 0) {
            project.getLogger()
                    .debug(
                            "Creating formatter that runs in bootstrapped JVM for java version {}",
                            JavaVersion.current());
            return loadBootstrappingJdkFormatter();
        }
        project.getLogger().debug("Creating formatter that runs in same JVM");
        return serviceLoadFormatter();
    }

    private FormatterService loadBootstrappingJdkFormatter() {
        Path javaExecPath = Jvm.current().getJavaExecutable().toPath();
        int javaMajorVersion = Integer.parseInt(JavaVersion.current().getMajorVersion());
        return new BootstrappingFormatterService(javaExecPath, javaMajorVersion, getJarLocations());
    }

    private FormatterService serviceLoadFormatter() {
        URL[] jarUris = getJarLocations().stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Unable to convert path to URL: " + path, e);
                    }
                })
                .toArray(URL[]::new);

        ClassLoader classLoader = new URLClassLoader(jarUris, FormatterService.class.getClassLoader());
        return Iterables.getOnlyElement(ServiceLoader.load(FormatterService.class, classLoader));
    }

    private List<Path> getJarLocations() {
        return configuration.getFiles().stream().map(File::toPath).collect(Collectors.toList());
    }
}
