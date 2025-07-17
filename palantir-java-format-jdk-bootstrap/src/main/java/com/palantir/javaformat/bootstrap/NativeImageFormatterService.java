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

package com.palantir.javaformat.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.Replacement;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

public class NativeImageFormatterService implements FormatterService {
    private static final ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new GuavaModule()).build();
    private final Path nativeImagePath;

    public NativeImageFormatterService(Path nativeImagePath) {
        this.nativeImagePath = nativeImagePath;
    }

    @Override
    public ImmutableList<Replacement> getFormatReplacements(String input, Collection<Range<Integer>> ranges) {
        try {
            FormatterNativeImageArgs command = FormatterNativeImageArgs.builder()
                    .nativeImagePath(nativeImagePath)
                    .outputReplacements(true)
                    .characterRanges(
                            ranges.stream().map(RangeUtils::toStringRange).collect(Collectors.toList()))
                    .build();

            Optional<String> output = FormatterCommandRunner.runWithStdin(
                    command.toArgs(), input, Optional.ofNullable(nativeImagePath.getParent()));
            if (output.isEmpty() || output.get().isEmpty()) {
                return ImmutableList.of();
            }
            return MAPPER.readValue(output.get(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Error running the native image command", e);
        }
    }

    @Override
    public String formatSourceReflowStringsAndFixImports(String input) {
        try {
            return runFormatterCommand(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Error running the native image command", e);
        }
    }

    @Override
    public String fixImports(String input) {
        try {
            return runFormatterCommand(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Error running the native image command", e);
        }
    }

    private String runFormatterCommand(String input) throws IOException {
        FormatterNativeImageArgs command = FormatterNativeImageArgs.builder()
                .nativeImagePath(nativeImagePath)
                .outputReplacements(false)
                .build();
        return FormatterCommandRunner.runWithStdin(
                        command.toArgs(), input, Optional.ofNullable(nativeImagePath.getParent()))
                .orElse(input);
    }

    @Value.Immutable
    interface FormatterNativeImageArgs {

        List<String> characterRanges();

        boolean outputReplacements();

        Path nativeImagePath();

        default List<String> toArgs() {
            ImmutableList.Builder<String> args = ImmutableList.<String>builder()
                    .add(nativeImagePath().toAbsolutePath().toString());

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

        static FormatterNativeImageArgs.Builder builder() {
            return new FormatterNativeImageArgs.Builder();
        }

        final class Builder extends ImmutableFormatterNativeImageArgs.Builder {}
    }
}
