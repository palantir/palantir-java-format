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

package com.palantir.javaformat.gradle.spotless;

import com.palantir.javaformat.java.FormatterService;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ServiceLoader;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.workers.WorkAction;

public abstract class FormatJavaWorkAction implements WorkAction<FormatJavaParameters> {

    private static final Logger logger = Logging.getLogger(FormatJavaWorkAction.class);

    @Override
    public void execute() {
        FormatJavaParameters params = getParameters();

        try {
            File inputFile = params.getInputFile().get().getAsFile();
            File outputFile = params.getOutputFile().get().getAsFile();

            String input = Files.readString(inputFile.toPath());

            FormatterService formatter = loadFormatterService();

            logger.debug("Formatting file with worker process: {}", inputFile.getName());

            String output = formatter.formatSourceReflowStringsAndFixImports(input);

            Files.writeString(outputFile.toPath(), output);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to format Java file", e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Formatting failed in worker daemon. This may indicate a bug in palantir-java-format "
                            + "or insufficient resources. Original error: " + e.getMessage(),
                    e);
        }
    }

    private FormatterService loadFormatterService() {
        ServiceLoader<FormatterService> serviceLoader =
                ServiceLoader.load(FormatterService.class, getClass().getClassLoader());

        return serviceLoader
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No FormatterService found. Ensure palantir-java-format is on the worker classpath."));
    }
}
