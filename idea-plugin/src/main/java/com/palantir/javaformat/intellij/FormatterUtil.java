/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.palantir.javaformat.intellij;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.intellij.openapi.util.TextRange;
import com.palantir.javaformat.java.FormatterExceptionApi;
import com.palantir.javaformat.java.FormatterService;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FormatterUtil {
    private static final Logger log = LoggerFactory.getLogger(FormatterUtil.class);

    static Map<TextRange, String> getReplacements(
            FormatterService formatter, String text, Collection<TextRange> ranges) {
        try {
            ImmutableMap.Builder<TextRange, String> replacements = ImmutableMap.builder();
            formatter.getFormatReplacements(text, toRanges(ranges)).forEach(replacement -> {
                replacements.put(toTextRange(replacement.getReplaceRange()), replacement.getReplacementString());
            });
            return replacements.build();
        } catch (FormatterExceptionApi e) {
            log.debug("Formatter failed, no replacements", e);
            return ImmutableMap.of();
        }
    }

    private static Collection<Range<Integer>> toRanges(Collection<TextRange> textRanges) {
        return textRanges.stream()
                .map(textRange -> Range.closedOpen(textRange.getStartOffset(), textRange.getEndOffset()))
                .collect(Collectors.toList());
    }

    private static TextRange toTextRange(Range<Integer> range) {
        checkState(range.lowerBoundType().equals(BoundType.CLOSED) && range.upperBoundType().equals(BoundType.OPEN));
        return new TextRange(range.lowerEndpoint(), range.upperEndpoint());
    }

    private FormatterUtil() {}
}
