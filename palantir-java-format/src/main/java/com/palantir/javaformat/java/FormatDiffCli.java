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

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
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

public final class FormatDiffCli {
    // each section in the git diff output starts like this
    private static final Pattern SEPARATOR = Pattern.compile("diff --git");

    // "+++ b/witchcraft-example-stateless/src/main/java/com/palantir/witchcraftexample/WitchcraftExampleResource.java"
    private static final Pattern FILENAME = Pattern.compile("^\\+\\+\\+ (.+?)/(?<filename>.+)\\n", Pattern.MULTILINE);

    // "@@ -25,6 +26,19 @@ public final class WitchcraftExampleServer {"
    private static final Pattern HUNK =
            Pattern.compile("^@@.*\\+(?<startLineOneIndexed>\\d+)(,(?<numLines>\\d+))?", Pattern.MULTILINE);

    public static void main(String[] _args) throws IOException, InterruptedException {
        formatFiles(Paths.get("."), Formatter.createFormatter(
                JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build()));
    }

    static void formatFiles(Path cwd, Formatter formatter) throws IOException, InterruptedException {
        String gitOutput = gitDiff(cwd);

        Splitter.on(SEPARATOR).omitEmptyStrings().splitToList(gitOutput).forEach(singleFileDiff -> {
            Matcher filenameMatcher = FILENAME.matcher(singleFileDiff);
            if (!filenameMatcher.find()) {
                System.err.println("Failed to find filename");
                return;
            }

            Path path = cwd.resolve(Paths.get(filenameMatcher.group("filename")));
            if (!Files.exists(path)) {
                System.err.println("Skipping non-existent file " + path);
                return; // i.e. a deleted file
            }
            String input;
            try {
                input = new String(Files.readAllBytes(path), UTF_8);
            } catch (IOException e) {
                System.err.println("Failed to read file " + path);
                e.printStackTrace(System.err);
                return;
            }

            RangeSet<Integer> lineRanges = TreeRangeSet.create();
            Matcher hunk = HUNK.matcher(singleFileDiff);
            while (hunk.find()) {
                int firstLineOfHunk = Integer.parseInt(hunk.group("startLineOneIndexed")) - 1;
                int hunkLength = Optional.ofNullable(hunk.group("numLines")).map(Integer::parseInt).orElse(1);
                Range<Integer> rangeZeroIndexed = Range.closedOpen(firstLineOfHunk, firstLineOfHunk + hunkLength);
                lineRanges.add(rangeZeroIndexed);
            }
            RangeSet<Integer> charRanges = Formatter.lineRangesToCharRanges(input, lineRanges);

            // TODO(dfox): only filter files ending in .java? or allow a regex to be passed in?

            try {
                System.err.println("Formatting " + path + charRanges);
                String output = formatter.formatSource(input, charRanges.asRanges());
                Files.write(path, output.getBytes(UTF_8));
            } catch (IOException | FormatterException e) {
                System.err.println("Failed to format file " + path);
                e.printStackTrace(System.err);
            }
        });
    }

    private static String gitDiff(Path cwd) throws IOException, InterruptedException {
        // TODO(dfox): this does nothing if working dir is clean - maybe use HEAD^ to format prev commit?
        Process process = new ProcessBuilder().command("git", "diff", "-U0", "HEAD").directory(cwd.toFile()).start();
        Preconditions.checkState(process.waitFor(10, TimeUnit.SECONDS), "git diff took too long to terminate");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteStreams.copy(process.getInputStream(), baos);
        return new String(baos.toByteArray(), UTF_8);
    }
}
