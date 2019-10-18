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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Streams;
import com.google.common.collect.TreeRangeSet;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class FormatDiffCli {
    // each section in the git diff output starts like this
    private static final Pattern SEPARATOR = Pattern.compile("diff --git");

    // "+++ b/witchcraft-example-stateless/src/main/java/com/palantir/witchcraftexample/WitchcraftExampleResource.java"
    private static final Pattern FILENAME = Pattern.compile("^\\+\\+\\+ (.+?)/(?<filename>.+)\\n", Pattern.MULTILINE);

    // "@@ -25,6 +26,19 @@ public final class WitchcraftExampleServer {"
    private static final Pattern HUNK =
            Pattern.compile("^@@.*\\+(?<startLineOneIndexed>\\d+)(,(?<numLines>\\d+))?", Pattern.MULTILINE);

    public static void main(String[] _args) throws IOException, InterruptedException {
        Formatter formatter = Formatter.createFormatter(
                JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());
        Path cwd = Paths.get(".");

        String gitOutput = gitDiff(cwd);
        parseGitDiffOutput(gitOutput)
                .filter(diff -> diff.path.toString().endsWith(".java"))
                .map(diff -> new SingleFileDiff(cwd.resolve(diff.path), diff.lineRanges))
                .filter(diff -> Files.exists(diff.path))
                .forEach(diff -> format(formatter, diff));
    }

    /** Parses the filenames and edited ranges out of `git diff -U0`. */
    @VisibleForTesting
    static Stream<SingleFileDiff> parseGitDiffOutput(String gitOutput) {
        return Streams.stream(Splitter.on(SEPARATOR).omitEmptyStrings().split(gitOutput)).flatMap(singleFileDiff -> {
            Matcher filenameMatcher = FILENAME.matcher(singleFileDiff);
            if (!filenameMatcher.find()) {
                System.err.println("Failed to find filename");
                return Stream.empty();
            }
            Path path = Paths.get(filenameMatcher.group("filename"));

            RangeSet<Integer> lineRanges = TreeRangeSet.create();
            Matcher hunk = HUNK.matcher(singleFileDiff);
            while (hunk.find()) {
                int firstLineOfHunk = Integer.parseInt(hunk.group("startLineOneIndexed")) - 1;
                int hunkLength = Optional.ofNullable(hunk.group("numLines")).map(Integer::parseInt).orElse(1);
                Range<Integer> rangeZeroIndexed = Range.closedOpen(firstLineOfHunk, firstLineOfHunk + hunkLength);
                lineRanges.add(rangeZeroIndexed);
            }

            return Stream.of(new SingleFileDiff(path, lineRanges));
        });
    }

    private static void format(Formatter formatter, SingleFileDiff diff) {
        String input;
        try {
            input = new String(Files.readAllBytes(diff.path), UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read file " + diff.path);
            e.printStackTrace(System.err);
            return;
        }

        RangeSet<Integer> charRanges = Formatter.lineRangesToCharRanges(input, diff.lineRanges);

        try {
            System.err.println("Formatting " + diff.path);
            String output = formatter.formatSource(input, charRanges.asRanges());
            Files.write(diff.path, output.getBytes(UTF_8));
        } catch (IOException | FormatterException e) {
            System.err.println("Failed to format file " + diff.path);
            e.printStackTrace(System.err);
        }
    }

    private static String gitDiff(Path cwd) throws IOException, InterruptedException {
        // TODO(dfox): this does nothing if working dir is clean - maybe use HEAD^ to format prev commit?
        Process process = new ProcessBuilder().command("git", "diff", "-U0", "HEAD").directory(cwd.toFile()).start();
        Preconditions.checkState(process.waitFor(10, TimeUnit.SECONDS), "git diff took too long to terminate");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteStreams.copy(process.getInputStream(), baos);
        return new String(baos.toByteArray(), UTF_8);
    }

    // TODO(dfox): replace this with immutables
    public static class SingleFileDiff {
        private final Path path;
        private final RangeSet<Integer> lineRanges; // zero-indexed

        public SingleFileDiff(Path path, RangeSet<Integer> lineRanges) {
            this.path = path;
            this.lineRanges = lineRanges;
        }

        @Override
        public String toString() {
            return "SingleFileDiff{path=" + path + ", lineRanges=" + lineRanges + '}';
        }
    }
}
