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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Streams;
import com.google.common.collect.TreeRangeSet;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.Replacement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class FormatDiff {
    // each section in the git diff output starts like this
    private static final Pattern SEPARATOR = Pattern.compile("diff --git");

    // "+++ b/witchcraft-example-stateless/src/main/java/com/palantir/witchcraftexample/WitchcraftExampleResource.java"
    private static final Pattern FILENAME = Pattern.compile("^\\+\\+\\+ (.+?)/(?<filename>.+)\\n", Pattern.MULTILINE);

    // "@@ -25,6 +26,19 @@ public final class WitchcraftExampleServer {"
    private static final Pattern HUNK =
            Pattern.compile("^@@.*\\+(?<startLineOneIndexed>\\d+)(,(?<numLines>\\d+))?", Pattern.MULTILINE);

    public static void formatDiff(Path dirToFormat, FormatterService formatter)
            throws IOException, InterruptedException {
        String gitOutput = gitDiff(dirToFormat);
        Path gitTopLevelDir = gitTopLevelDir(dirToFormat);

        parseGitDiffOutput(gitOutput)
                .filter(diff -> diff.path.toString().endsWith(".java"))
                .map(diff -> new SingleFileDiff(gitTopLevelDir.resolve(diff.path), diff.lineRanges))
                .filter(diff -> Files.exists(diff.path))
                .forEach(diff -> {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    format(formatter, diff);
                    Duration duration = stopwatch.stop().elapsed();
                    if (duration.toMillis() > Duration.ofMillis(500).toMillis()) {
                        System.err.println(
                                "Large file detected: " + diff.path + " duration in second: " + duration.toSeconds());
                    }
                });
    }

    /** Parses the filenames and edited ranges out of `git diff -U0`. */
    @VisibleForTesting
    static Stream<SingleFileDiff> parseGitDiffOutput(String gitOutput) {
        return Streams.stream(Splitter.on(SEPARATOR).omitEmptyStrings().split(gitOutput))
                .flatMap(singleFileDiff -> {
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
                        int hunkLength = Optional.ofNullable(hunk.group("numLines"))
                                .map(Integer::parseInt)
                                .orElse(1);
                        Range<Integer> rangeZeroIndexed =
                                Range.closedOpen(firstLineOfHunk, firstLineOfHunk + hunkLength);
                        lineRanges.add(rangeZeroIndexed);
                    }

                    return Stream.of(new SingleFileDiff(path, lineRanges));
                });
    }

    private static void format(FormatterService formatter, SingleFileDiff diff) {
        String input;
        try {
            input = new String(Files.readAllBytes(diff.path), UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read file " + diff.path);
            e.printStackTrace(System.err);
            return;
        }

        RangeSet<Integer> charRanges = lineRangesToCharRanges(input, diff.lineRanges);

        try {
            System.err.println("Formatting " + diff.path);
            ImmutableList<Replacement> replacements = formatter.getFormatReplacements(input, charRanges.asRanges());
            String output = applyReplacements(input, replacements);
            Files.write(diff.path, output.getBytes(UTF_8));
        } catch (IOException | FormatterException e) {
            System.err.println("Failed to format file " + diff.path);
            e.printStackTrace(System.err);
        }
    }

    private static String gitDiff(Path dir) throws IOException, InterruptedException {
        // TODO(dfox): this does nothing if working dir is clean - maybe use HEAD^ to format prev commit?
        return gitCommand(
                dir, "git", "diff", "-U0", "HEAD", dir.toAbsolutePath().toString());
    }

    private static Path gitTopLevelDir(Path dir) throws IOException, InterruptedException {
        return Paths.get(gitCommand(dir, "git", "rev-parse", "--show-toplevel"));
    }

    private static String gitCommand(Path dir, String... args) throws IOException, InterruptedException {
        Process process =
                new ProcessBuilder().command(args).directory(dir.toFile()).start();

        Preconditions.checkState(process.waitFor(30, TimeUnit.SECONDS), "git diff took too long to terminate");
        Preconditions.checkState(process.exitValue() == 0, "Expected return code of 0");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        process.getInputStream().transferTo(baos);
        return new String(baos.toByteArray(), UTF_8).trim();
    }

    private static String applyReplacements(String input, Collection<Replacement> replacementsCollection) {
        List<Replacement> replacements = new ArrayList<>(replacementsCollection);
        replacements.sort(comparing((Replacement r) -> r.getReplaceRange().lowerEndpoint())
                .reversed());
        StringBuilder writer = new StringBuilder(input);
        for (Replacement replacement : replacements) {
            writer.replace(
                    replacement.getReplaceRange().lowerEndpoint(),
                    replacement.getReplaceRange().upperEndpoint(),
                    replacement.getReplacementString());
        }
        return writer.toString();
    }

    /** Converts zero-indexed, [closed, open) line ranges in the given source file to character ranges. */
    private static RangeSet<Integer> lineRangesToCharRanges(String input, RangeSet<Integer> lineRanges) {
        List<Integer> lines = new ArrayList<>();
        Iterators.addAll(lines, new LineOffsetIterator(input));
        lines.add(input.length() + 1);

        final RangeSet<Integer> characterRanges = TreeRangeSet.create();
        for (Range<Integer> lineRange :
                lineRanges.subRangeSet(Range.closedOpen(0, lines.size() - 1)).asRanges()) {
            int lineStart = lines.get(lineRange.lowerEndpoint());
            // Exclude the trailing newline. This isn't strictly necessary, but handling blank lines
            // as empty ranges is convenient.
            int lineEnd = lines.get(lineRange.upperEndpoint()) - 1;
            Range<Integer> range = Range.closedOpen(lineStart, lineEnd);
            characterRanges.add(range);
        }
        return characterRanges;
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
