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

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import java.util.Collection;
import java.util.function.Function;

@AutoService(FormatterService.class)
public final class FormatterServiceImpl implements FormatterService {

    private final Formatter formatter;
    private final JavaFormatterOptions options;

    public FormatterServiceImpl() {
        this(JavaFormatterOptions.builder().style(Style.PALANTIR).build());
    }

    private FormatterServiceImpl(JavaFormatterOptions options) {
        formatter = Formatter.createFormatter(options);
        this.options = options;
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

    @Override
    public FormatterService withOptions(
            Function<? super JavaFormatterOptions, ? extends JavaFormatterOptions> optionsTransformer) {
        JavaFormatterOptions newOptions = optionsTransformer.apply(options);
        return new FormatterServiceImpl(newOptions);
    }

    @Override
    public String fixImports(String input) throws FormatterException {
        return formatter.fixImports(input);
    }
}
