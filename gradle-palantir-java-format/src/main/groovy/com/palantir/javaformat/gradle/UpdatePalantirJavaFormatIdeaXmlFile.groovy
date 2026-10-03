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

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class UpdatePalantirJavaFormatIdeaXmlFile extends DefaultTask {

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getImplementationConfig();

    @org.gradle.api.tasks.Optional
    @InputFiles
    public abstract ConfigurableFileCollection getNativeImageConfig();

    @org.gradle.api.tasks.Optional
    @OutputFile
    public abstract RegularFileProperty getXmlOutputFile();

    @org.gradle.api.tasks.Optional
    @OutputFile
    public abstract RegularFileProperty getNativeImageOutputFile();

    @TaskAction
    public final void updateXml() {
        List<URI> uris =
                getImplementationConfig().getFiles().stream().map(File::toURI).toList();
        Optional<URI> nativeUri = getNativeImageConfig().getFiles().stream()
                .findFirst()
                .map(File::toURI)
                .map(uri -> NativeImageAtomicCopy.copyToCacheDir(
                        Paths.get(uri),
                        getNativeImageOutputFile().getAsFile().get().toPath()));
        XmlUtils.updateIdeaXmlFile(
                getXmlOutputFile().getAsFile().get(),
                node -> ConfigureJavaFormatterXml.configureJavaFormat(node, uris, nativeUri));
    }
}
