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

package com.palantir.java.javaformat.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.bootstrap.BootstrappingFormatterService;
import com.palantir.javaformat.java.Replacement;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class BootstrappingFormatterServiceTest {

    @Test
    void can_format_file_with_replacement() {
        String input = getTestResourceContent("format.input");
        String expectedOutput = getTestResourceContent("format.output");

        ImmutableList<Replacement> replacements =
                getFormatter().getFormatReplacements(input, List.of(Range.open(0, input.length())));

        assertThat(replacements).hasSize(1);
        assertThat(replacements.get(0).getReplacementString()).isEqualTo(expectedOutput);
    }

    @Test
    void can_format_full_file() {
        String input = getTestResourceContent("format.input");
        String expectedOutput = getTestResourceContent("format.output");

        String formatted = getFormatter().formatSourceReflowStringsAndFixImports(input);

        assertThat(formatted).isEqualTo(expectedOutput);
    }

    private BootstrappingFormatterService getFormatter() {
        return new BootstrappingFormatterService(
                javaBinPath(), Runtime.version().feature(), getClasspath());
    }

    private String getTestResourceContent(String resourceName) {
        try {
            URI resource = getClass().getClassLoader().getResource(resourceName).toURI();
            return Files.readString(Path.of(resource));
        } catch (Exception e) {
            throw new RuntimeException("Error reading resource: " + resourceName, e);
        }
    }

    private static List<Path> getClasspath() {
        String classpath = System.getProperty("java.class.path");
        return Splitter.on(':')
                .trimResults()
                .omitEmptyStrings()
                .splitToStream(classpath)
                .map(Path::of)
                .collect(Collectors.toList());
    }

    private static Path javaBinPath() {
        String javaHome = Preconditions.checkNotNull(System.getProperty("java.home"), "java.home property not set");
        return Path.of(javaHome).resolve("bin").resolve("java");
    }
}
