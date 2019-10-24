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

import groovy.util.Node;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;

public class ConfigurePalantirJavaFormatXml extends UpdateIntellijXmlTask {
    private final Property<Configuration> implConfiguration = getProject().getObjects().property(Configuration.class);

    @Classpath
    Property<Configuration> getImplConfiguration() {
        return implConfiguration;
    }

    public ConfigurePalantirJavaFormatXml() {
        getXmlFile().set(getProject().file(".idea/palantir-java-format.xml"));
    }

    @Override
    protected final void configure(Node rootNode) {
        List<URI> uris = implConfiguration.get().getFiles().stream().map(File::toURI).collect(Collectors.toList());
        ConfigureJavaFormatterXml.configureJavaFormat(rootNode, uris);
    }
}
