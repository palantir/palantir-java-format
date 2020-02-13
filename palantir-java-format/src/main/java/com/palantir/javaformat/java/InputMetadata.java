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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.OpsBuilder.BlankLineWanted;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * Records metadata about the input, namely existing blank lines that we might want to preserve, as well as what ranges
 * can be partially formatted.
 */
@Immutable
@Value.Immutable
@Value.Style(overshadowImplementation = true)
public interface InputMetadata {
    /** Remembers preferences from the input about whether blank lines are wanted or not at a given token index. */
    ImmutableMap<Integer, BlankLineWanted> blankLines();

    /**
     * Marks regions that can be partially formatted, used to determine the actual ranges that will be formatted when
     * ranges are requested.
     */
    @Default
    default ImmutableRangeSet<Integer> partialFormatRanges() {
        return ImmutableRangeSet.of();
    }
}
