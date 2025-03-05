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

package com.palantir.javaformat;

import static java.util.Comparator.comparing;

import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.palantir.javaformat.java.Replacement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Utils {
    private Utils() {}

    public static String applyReplacements(String input, Collection<Replacement> replacementsCollection) {
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
    public static RangeSet<Integer> lineRangesToCharRanges(String input, RangeSet<Integer> lineRanges) {
        List<Integer> lines = new ArrayList<>();
        Iterators.addAll(lines, Newlines.lineOffsetIterator(input));
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
}
