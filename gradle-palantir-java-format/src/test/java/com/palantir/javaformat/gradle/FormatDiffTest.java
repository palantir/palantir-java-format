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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.palantir.javaformat.bootstrap.BootstrappingFormatterService;
import com.palantir.javaformat.bootstrap.NativeImageFormatterService;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.JavaFormatterOptions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FormatDiffTest {

    private static final File CLASSPATH_FILE = new File("build/impl.classpath");
    private static final File NATIVE_IMAGE_FILE = new File("build/nativeImage.path");

    @TempDir
    Path repo;

    @Test
    void parsing_git_diff_output_works() throws IOException {
        String example1 = Files.readString(
                Paths.get("src/test/resources/com/palantir/javaformat/java/FormatDiffCliTest/example1.patch"));

        List<String> strings = FormatDiff.parseGitDiffOutput(example1)
                .map(FormatDiff.SingleFileDiff::toString)
                .collect(Collectors.toList());
        assertThat(strings)
                .containsExactly(
                        "SingleFileDiff{path=build.gradle, lineRanges=[[24..25), [29..30)]}",
                        "SingleFileDiff{path=tracing/src/test/java/com/palantir/tracing/TracersTest.java, "
                                + "lineRanges=[[659..660), [675..676)]}");
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void reformat_a_subpath_of_a_git_directory_for_only_changed_lines(FormatterService formatterService)
            throws IOException, InterruptedException {
        runCommandInRepo("git", "init");
        runCommandInRepo("git", "config", "user.name", "Test User");
        runCommandInRepo("git", "config", "user.email", "test-user@palantir.com");
        runCommandInRepo("git", "config", "commit.gpgsign", "false");
        runCommandInRepo("git", "commit", "--allow-empty", "-m", "Init");

        Path subdir = repo.resolve("subdir");
        Files.createDirectories(subdir);

        Path reformatMe = subdir.resolve("ReformatMe.java");
        Files.write(reformatMe, ImmutableList.of("                                 class ReformatMe {}"), UTF_8);

        Path dontTouchMe = repo.resolve("DontTouchMe.java");
        Files.write(dontTouchMe, ImmutableList.of("                                 class DontTouchMe {}"), UTF_8);

        runCommandInRepo("git", "add", "-N", ".");

        FormatDiff.formatDiff(subdir, formatterService);

        assertThat(reformatMe).hasContent("class ReformatMe {}");
        assertThat(dontTouchMe).hasContent("                                 class DontTouchMe {}");
    }

    private void runCommandInRepo(String... args) throws IOException, InterruptedException {
        Process process =
                new ProcessBuilder().command(args).directory(repo.toFile()).start();

        Preconditions.checkState(process.waitFor(10, TimeUnit.SECONDS), "git diff took too long to terminate");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        process.getErrorStream().transferTo(baos);
        String stderr = baos.toString(UTF_8);

        Preconditions.checkState(process.exitValue() == 0, "Expected return code of 0: " + stderr);
    }

    private static Stream<FormatterService> getFormatters() throws IOException {
        JavaFormatterOptions options = JavaFormatterOptions.builder().build();
        return Stream.of(
                new BootstrappingFormatterService(
                        javaBinPath(), Runtime.version().feature(), getClasspath(), options),
                new NativeImageFormatterService(
                        Path.of(Files.readString(NATIVE_IMAGE_FILE.toPath()).trim()), options));
    }

    private static List<Path> getClasspath() throws IOException {
        return Splitter.on(':')
                .trimResults()
                .omitEmptyStrings()
                .splitToStream(Files.readString(CLASSPATH_FILE.toPath()))
                .map(Path::of)
                .collect(Collectors.toList());
    }

    private static Path javaBinPath() {
        String javaHome = Preconditions.checkNotNull(System.getProperty("java.home"), "java.home property not set");
        return Path.of(javaHome).resolve("bin").resolve("java");
    }
}
