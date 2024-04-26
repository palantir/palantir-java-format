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

package com.palantir.javaformat.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Joiner;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.Replacement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

public final class BootstrappingFormatterService implements FormatterService {
    private static final ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new GuavaModule()).build();

    private static final String FORMATTER_MAIN_CLASS = "com.palantir.javaformat.java.Main";

    private final Path jdkPath;
    private final Integer jdkMajorVersion;
    private final List<Path> implementationClassPath;

    public BootstrappingFormatterService(Path jdkPath, Integer jdkMajorVersion, List<Path> implementationClassPath) {
        this.jdkPath = jdkPath;
        this.jdkMajorVersion = jdkMajorVersion;
        this.implementationClassPath = implementationClassPath;
    }

    @Override
    public ImmutableList<Replacement> getFormatReplacements(String input, Collection<Range<Integer>> ranges) {
        try {
            return getFormatReplacementsInternal(input, ranges);
        } catch (IOException e) {
            throw new RuntimeException("Error running formatter command", e);
        }
    }

    @Override
    public String formatSourceReflowStringsAndFixImports(String input) {
        try {
            return runFormatterCommand(input);
        } catch (IOException e) {
            throw new RuntimeException("Error running formatter command", e);
        }
    }

    @Override
    public String fixImports(String input) throws FormatterException {
        try {
            return runFormatterCommand(input);
        } catch (IOException e) {
            throw new RuntimeException("Error running formatter command", e);
        }
    }

    private ImmutableList<Replacement> getFormatReplacementsInternal(String input, Collection<Range<Integer>> ranges)
            throws IOException {
        FormatterCliArgs command = FormatterCliArgs.builder()
                .jdkPath(jdkPath)
                .withJvmArgsForVersion(jdkMajorVersion)
                .implementationClasspath(implementationClassPath)
                .outputReplacements(true)
                .characterRanges(ranges.stream()
                        .map(BootstrappingFormatterService::toStringRange)
                        .collect(Collectors.toList()))
                .build();

        Optional<String> output = FormatterCommandRunner.runWithStdin(command.toArgs(), input);
        if (output.isEmpty() || output.get().isEmpty()) {
            return ImmutableList.of();
        }
        return MAPPER.readValue(output.get(), new TypeReference<>() {});
    }

    private String runFormatterCommand(String input) throws IOException {
        FormatterCliArgs command = FormatterCliArgs.builder()
                .jdkPath(jdkPath)
                .withJvmArgsForVersion(jdkMajorVersion)
                .implementationClasspath(implementationClassPath)
                .outputReplacements(false)
                .build();
        return FormatterCommandRunner.runWithStdin(command.toArgs(), input).orElse(input);
    }

    /** Returns a range representation as parsed by "com.palantir.javaformat.java.CommandLineOptionsParser". */
    private static String toStringRange(Range<Integer> range) {
        int lower = range.lowerBoundType() == BoundType.CLOSED ? range.lowerEndpoint() : range.lowerEndpoint() + 1;
        int higher = range.upperBoundType() == BoundType.CLOSED ? range.upperEndpoint() : range.upperEndpoint() - 1;
        if (lower == higher) {
            return String.valueOf(lower);
        }
        return String.format("%s:%s", lower, higher);
    }

    @Value.Immutable
    interface FormatterCliArgs {
        Path jdkPath();

        List<Path> implementationClasspath();

        List<String> characterRanges();

        List<String> jvmArgs();

        boolean outputReplacements();

        default List<String> toArgs() {
            ImmutableList.Builder<String> args = ImmutableList.<String>builder()
                    .add(jdkPath().toAbsolutePath().toString())
                    .addAll(jvmArgs())
                    .add(
                            "-cp",
                            implementationClasspath().stream()
                                    .map(path -> path.toAbsolutePath().toString())
                                    .collect(Collectors.joining(System.getProperty("path.separator"))))
                    .add(FORMATTER_MAIN_CLASS);

            if (!characterRanges().isEmpty()) {
                args.add("--character-ranges", Joiner.on(',').join(characterRanges()));
            }
            if (outputReplacements()) {
                args.add("--output-replacements");
            }

            return args
                    // Use palantir style
                    .add("--palantir")
                    // Trailing "-" enables formatting stdin -> stdout
                    .add("-")
                    .build();
        }

        static Builder builder() {
            return new Builder();
        }

        final class Builder extends ImmutableFormatterCliArgs.Builder {
            Builder withJvmArgsForVersion(Integer majorJvmVersion) {
                if (majorJvmVersion >= 16) {
                    addJvmArgs(
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");
                }
                return this;
            }
        }
    }
}
