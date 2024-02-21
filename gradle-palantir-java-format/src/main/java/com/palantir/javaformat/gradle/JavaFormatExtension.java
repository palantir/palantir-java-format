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
import com.palantir.javaformat.java.FormatterService;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import org.gradle.api.artifacts.Configuration;

public class JavaFormatExtension {
    private final Configuration configuration;
    private final Supplier<FormatterService> memoizedService;

    public JavaFormatExtension(Configuration configuration) {
        this.configuration = configuration;
        this.memoizedService = Suppliers.memoize(this::serviceLoadInternal);
    }

    public FormatterService serviceLoad() {
        return memoizedService.get();
    }

    private FormatterService serviceLoadInternal() {
        URL[] jarUris = configuration.getFiles().stream()
                .map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Unable to convert URI to URL: " + file, e);
                    }
                })
                .toArray(URL[]::new);

        ClassLoader classLoader = new URLClassLoader(jarUris, FormatterService.class.getClassLoader());
        FormatterService loadedFormatter =
                Iterables.getOnlyElement(ServiceLoader.load(FormatterService.class, classLoader));
        throw new RuntimeException(
                "Loaded formatter of type: " + loadedFormatter.getClass().getName());
    }
}
