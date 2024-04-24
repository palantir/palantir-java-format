/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.palantir.javaformat.java.FormatterService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PalantirJavaFormatFormattingService extends AsyncDocumentFormattingService {
    private static final Logger log = LoggerFactory.getLogger(PalantirJavaFormatFormattingService.class);
    private final FormatterProvider formatterProvider = new FormatterProvider();

    @Override
    protected FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest request) {
        Project project = request.getContext().getProject();
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        Optional<FormatterService> formatter = formatterProvider.get(project, settings);
    }

    @Override
    protected @NotNull String getNotificationGroupId() {
        return null;
    }

    @Override
    protected @NotNull @NlsSafe String getName() {
        return null;
    }

    @Override
    public @NotNull Set<Feature> getFeatures() {
        return null;
    }

    @Override
    public boolean canFormat(@NotNull PsiFile file) {
        return false;
    }

    private static final class PalantirJavaFormatFormattingTask implements FormattingTask {
        private final AsyncFormattingRequest request;
        private final Optional<FormatterService> formatterService;

        public PalantirJavaFormatFormattingTask(
                AsyncFormattingRequest request, Optional<FormatterService> formatterService) {
            this.request = request;
            this.formatterService = formatterService;
        }

        @Override
        public void run() {
            if (formatterService.isEmpty()) {
                request.onError("palantir-java-format", "Failed to format file with palantir-java-format");
                return;
            }
        }

        private static Collection<Range<Integer>> toRanges(AsyncFormattingRequest request) {
            if (isWholeFile(request)) {
                // The IDE sometimes passes invalid ranges when the file is unsaved before invoking the
                // formatter. So this is a workaround for that issue.
                return ImmutableList.of(
                        Range.closedOpen(0, request.getDocumentText().length()));
            }
            return request.getFormattingRanges().stream()
                    .map(textRange -> Range.closedOpen(textRange.getStartOffset(), textRange.getEndOffset()))
                    .collect(ImmutableList.toImmutableList());
        }

        private static boolean isWholeFile(AsyncFormattingRequest request) {
            List<TextRange> ranges = request.getFormattingRanges();
            return ranges.size() == 1
                    && ranges.get(0).getStartOffset() == 0
                    // using greater than or equal because ranges are sometimes passed inaccurately
                    && ranges.get(0).getEndOffset() >= request.getDocumentText().length();
        }

        @Override
        public boolean isRunUnderProgress() {
            return true;
        }

        @Override
        public boolean cancel() {
            return false;
        }
    }
}
