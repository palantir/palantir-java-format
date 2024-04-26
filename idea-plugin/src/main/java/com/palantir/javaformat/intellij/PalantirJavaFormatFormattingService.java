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

import static java.util.Comparator.comparing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.Replacement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PalantirJavaFormatFormattingService extends AsyncDocumentFormattingService {
    private static final Logger log = LoggerFactory.getLogger(PalantirJavaFormatFormattingService.class);
    private final FormatterProvider formatterProvider = new FormatterProvider();

    @Override
    protected FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest request) {
        Project project = request.getContext().getProject();
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        Optional<FormatterService> formatter = formatterProvider.get(project, settings);
        return new PalantirJavaFormatFormattingTask(request, formatter);
    }

    @Override
    protected @NotNull String getNotificationGroupId() {
        return Notifications.PARSING_ERROR_NOTIFICATION_GROUP;
    }

    @Override
    protected @NotNull @NlsSafe String getName() {
        return "palantir-java-format";
    }

    @Override
    public @NotNull Set<Feature> getFeatures() {
        return Set.of(Feature.AD_HOC_FORMATTING, Feature.FORMAT_FRAGMENTS, Feature.OPTIMIZE_IMPORTS);
    }

    @Override
    public boolean canFormat(@NotNull PsiFile file) {
        return JavaFileType.INSTANCE.equals(file.getFileType())
                && PalantirJavaFormatSettings.getInstance(file.getProject()).isEnabled();
    }

    @Override
    public @NotNull Set<ImportOptimizer> getImportOptimizers(@NotNull PsiFile file) {
        Project project = file.getProject();
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        Optional<FormatterService> formatter = formatterProvider.get(project, settings);
        return Set.of(new PalantirJavaFormatImportOptimizer(formatter));
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
                request.onError(
                        Notifications.GENERIC_ERROR_NOTIFICATION_GROUP,
                        "Failed to format file because formatterService is not configured");
                return;
            }

            try {
                String formattedText = applyReplacements(
                        request.getDocumentText(),
                        formatterService.get().getFormatReplacements(request.getDocumentText(), toRanges(request)));
                request.onTextReady(formattedText);
            } catch (FormatterException e) {
                request.onError(
                        Notifications.PARSING_ERROR_TITLE,
                        Notifications.parsingErrorMessage(
                                request.getContext().getContainingFile().getName()));
            }
        }

        public static String applyReplacements(String input, Collection<Replacement> replacementsCollection) {
            List<Replacement> replacements = new ArrayList<>(replacementsCollection);
            replacements.sort(comparing((Replacement r) -> r.getReplaceRange().lowerEndpoint())
                    .reversed());
            StringBuilder writer = new StringBuilder(input);
            for (Replacement replacement : replacements) {
                writer.replace(
                        replacement.getReplaceRange().lowerEndpoint(),
                        replacement.getReplaceRange().upperEndpoint(),
                        replacement.getReplacementString());
            }
            return writer.toString();
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
