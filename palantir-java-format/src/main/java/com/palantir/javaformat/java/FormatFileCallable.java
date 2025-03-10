/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.palantir.javaformat.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.palantir.javaformat.Utils;
import java.util.concurrent.Callable;

/** Encapsulates information about a file to be formatted, including which parts of the file to format. */
class FormatFileCallable implements Callable<String> {
    private final ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new GuavaModule()).build();

    private final String input;
    private final CommandLineOptions parameters;
    private final JavaFormatterOptions options;

    public FormatFileCallable(CommandLineOptions parameters, String input, JavaFormatterOptions options) {
        this.input = input;
        this.parameters = parameters;
        this.options = options;
    }

    @Override
    public String call() throws FormatterException {
        if (parameters.fixImportsOnly()) {
            return fixImports(input);
        }

        Formatter formatter = Formatter.createFormatter(options);
        if (parameters.outputReplacements()) {
            return formatReplacements(formatter);
        }
        return formatFile(formatter);
    }

    private String formatReplacements(Formatter formatter) throws FormatterException {
        ImmutableList<Replacement> replacements =
                formatter.getFormatReplacements(input, characterRanges(input).asRanges());
        try {
            return MAPPER.writeValueAsString(replacements);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing replacement output", e);
        }
    }

    private String formatFile(Formatter formatter) throws FormatterException {
        String formatted = formatter.formatSource(input, characterRanges(input).asRanges());
        formatted = fixImports(formatted);
        if (parameters.reflowLongStrings()) {
            formatted = StringWrapper.wrap(options.maxLineLength(), formatted, formatter);
        }
        return formatted;
    }

    private String fixImports(String input) throws FormatterException {
        if (parameters.removeUnusedImports()) {
            input = RemoveUnusedImports.removeUnusedImports(input);
        }
        if (parameters.sortImports()) {
            input = ImportOrderer.reorderImports(input, options.style());
        }
        return input;
    }

    private RangeSet<Integer> characterRanges(String input) {
        final RangeSet<Integer> characterRanges = TreeRangeSet.create();

        if (parameters.characterRanges().isEmpty()
                && parameters.lines().isEmpty()
                && parameters.offsets().isEmpty()) {
            characterRanges.add(Range.closedOpen(0, input.length()));
            return characterRanges;
        }

        if (!parameters.characterRanges().isEmpty()) {
            characterRanges.addAll(parameters.characterRanges());
        }

        if (!parameters.lines().isEmpty()) {
            characterRanges.addAll(Utils.lineRangesToCharRanges(input, parameters.lines()));
        }

        for (int i = 0; i < parameters.offsets().size(); i++) {
            Integer length = parameters.lengths().get(i);
            if (length == 0) {
                // 0 stands for "format the line under the cursor"
                length = 1;
            }
            characterRanges.add(Range.closedOpen(
                    parameters.offsets().get(i), parameters.offsets().get(i) + length));
        }

        return characterRanges;
    }
}
