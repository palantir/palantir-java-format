package com.palantir.javaformat.java;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import java.util.Collection;

@AutoService(FormatterService.class)
public final class FormatterServiceImpl implements FormatterService {

    private final Formatter formatter;

    public FormatterServiceImpl() {
        JavaFormatterOptions options =
                JavaFormatterOptions.builder().style(Style.PALANTIR).build();
        formatter = Formatter.createFormatter(options);
    }

    @Override
    public ImmutableList<Replacement> getFormatReplacements(String text, Collection<Range<Integer>> toRanges)
            throws FormatterException {
        return formatter.getFormatReplacements(text, toRanges);
    }

    @Override
    public String formatSourceReflowStringsAndFixImports(String input) throws FormatterException {
        return formatter.formatSourceAndFixImports(input);
    }
}
