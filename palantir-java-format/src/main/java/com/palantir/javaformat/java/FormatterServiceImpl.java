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
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Stopwatch stopwatch = Stopwatch.createStarted();
        String output = formatter.formatSourceAndFixImports(input);
        stopwatch.stop();

        if (stopwatch.elapsed(TimeUnit.MILLISECONDS) > 200) {
            Pattern pattern = Pattern.compile("(record|enum|class|interface) ([A-Z][a-zA-Z0-9]+) ");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                System.out.println(stopwatch.elapsed(TimeUnit.MILLISECONDS) + " on class: " + matcher.group(2));
            } else {
                System.out.println(stopwatch.elapsed(TimeUnit.MILLISECONDS) + " on unknown class: " + output);
            }
        }
        return output;
    }
}
