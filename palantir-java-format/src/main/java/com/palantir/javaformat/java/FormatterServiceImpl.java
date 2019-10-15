package com.palantir.javaformat.java;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;

@AutoService(FormatterService.class)
public final class FormatterServiceImpl implements FormatterService {

    @Override
    public ImmutableList<Replacement> getFormatReplacements(
            JavaFormatterOptions options, String text, Collection<Range<Integer>> toRanges) throws FormatterException {

        return Formatter.createFormatter(options).getFormatReplacements(text, toRanges);
    }
}
