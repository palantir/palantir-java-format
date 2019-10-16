package com.palantir.javaformat.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;

/**
 * A stable facade for palantir-java-format. The implementation must be ServiceLoaded, to ensure its classpath remains
 * isolated.
 */
public interface FormatterService {

    /**
     * Emit a list of {@link Replacement}s to convert from input to formatted output.
     *
     * @param input the input compilation unit
     * @param ranges the character ranges to reformat
     * @return a list of {@link Replacement}s, sorted from low index to high index, without overlaps
     * @throws FormatterException if the input string cannot be parsed
     */
    ImmutableList<Replacement> getFormatReplacements(
            JavaFormatterOptions options, String input, Collection<Range<Integer>> ranges) throws FormatterException;
}
