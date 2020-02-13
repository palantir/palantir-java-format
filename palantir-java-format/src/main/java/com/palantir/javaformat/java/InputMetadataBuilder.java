/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.palantir.javaformat.Input;
import com.palantir.javaformat.OpsBuilder.BlankLineWanted;
import java.util.HashMap;
import java.util.Map;

public final class InputMetadataBuilder {
    private final Map<Integer, BlankLineWanted> blankLines = new HashMap<>(); // Info on blank lines.
    private final RangeSet<Integer> partialFormatRanges = TreeRangeSet.create();

    /**
     * A blank line is or is not wanted here.
     *
     * @param k the {@link Input.Tok} index
     * @param wanted whether a blank line is wanted here
     */
    public void blankLine(int k, BlankLineWanted wanted) {
        if (blankLines.containsKey(k)) {
            blankLines.put(k, blankLines.get(k).merge(wanted));
        } else {
            blankLines.put(k, wanted);
        }
    }

    /** Marks a region that can be partially formatted. */
    public void markForPartialFormat(Input.Token start, Input.Token end) {
        int lo = JavaOutput.startTok(start).getIndex();
        int hi = JavaOutput.endTok(end).getIndex();
        partialFormatRanges.add(Range.closed(lo, hi));
    }

    public InputMetadata build() {
        return ImmutableInputMetadata.builder()
                .blankLines(ImmutableMap.copyOf(blankLines))
                .partialFormatRanges(ImmutableRangeSet.copyOf(partialFormatRanges))
                .build();
    }
}
