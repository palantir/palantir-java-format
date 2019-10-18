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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FormatDiffCliTest {

    @Test
    void parsing_git_diff_output_works() throws IOException {
        String example1 = new String(
                Files.readAllBytes(
                        Paths.get("src/test/resources/com/palantir/javaformat/java/FormatDiffCliTest/example1.patch")),
                StandardCharsets.UTF_8);

        List<String> strings = FormatDiffCli.parseGitDiffOutput(example1)
                .map(FormatDiffCli.SingleFileDiff::toString)
                .collect(Collectors.toList());
        assertEquals(
                ImmutableList.of(
                        "SingleFileDiff{path=build.gradle, lineRanges=[[24..25), [29..30)]}",
                        "SingleFileDiff{path=tracing/src/test/java/com/palantir/tracing/TracersTest.java, "
                                + "lineRanges=[[659..660), [675..676)]}"),
                strings);
    }
}
