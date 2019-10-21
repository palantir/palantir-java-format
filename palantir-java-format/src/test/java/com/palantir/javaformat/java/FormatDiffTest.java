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

package com.palantir.javaformat.java;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FormatDiffTest {
    @TempDir Path repo;

    @Test
    void parsing_git_diff_output_works() throws IOException {
        String example1 = new String(
                Files.readAllBytes(
                        Paths.get("src/test/resources/com/palantir/javaformat/java/FormatDiffCliTest/example1.patch")),
                StandardCharsets.UTF_8);

        List<String> strings = FormatDiff.parseGitDiffOutput(example1).map(FormatDiff.SingleFileDiff::toString).collect(
                Collectors.toList());
        assertEquals(
                ImmutableList.of(
                        "SingleFileDiff{path=build.gradle, lineRanges=[[24..25), [29..30)]}",
                        "SingleFileDiff{path=tracing/src/test/java/com/palantir/tracing/TracersTest.java, "
                                + "lineRanges=[[659..660), [675..676)]}"),
                strings);
    }

    @Test
    void reformat_a_subpath_of_a_git_directory_for_only_changed_lines() throws IOException, InterruptedException {
        runCommandInRepo("git", "init");
        runCommandInRepo("git", "commit", "--allow-empty", "-m", "Init");

        Path subdir = repo.resolve("subdir");
        Files.createDirectories(subdir);

        Path reformatMe = subdir.resolve("ReformatMe.java");
        Files.write(reformatMe, ImmutableList.of("                                 class ReformatMe {}"), UTF_8);

        Path dontTouchMe = repo.resolve("DontTouchMe.java");
        Files.write(dontTouchMe, ImmutableList.of("                                 class DontTouchMe {}"), UTF_8);

        runCommandInRepo("git", "add", "-N", ".");

        FormatDiff.formatDiff(subdir);

        assertThat(reformatMe).hasContent("class ReformatMe {}");
        assertThat(dontTouchMe).hasContent("                                 class DontTouchMe {}");
    }

    private void runCommandInRepo(String... args) throws IOException, InterruptedException {
        Process process = new ProcessBuilder().command(args).directory(repo.toFile()).start();

        Preconditions.checkState(process.waitFor(10, TimeUnit.SECONDS), "git diff took too long to terminate");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteStreams.copy(process.getErrorStream(), baos);
        String stderr = new String(baos.toByteArray(), UTF_8);

        Preconditions.checkState(process.exitValue() == 0, "Expected return code of 0: " + stderr);
    }
}
