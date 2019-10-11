package com.palantir.javaformat.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;

public interface FormatterService {

    ImmutableList<Replacement> getFormatReplacements(String text, Collection<Range<Integer>> toRanges)
        throws FormatterExceptionApi;
}
