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
