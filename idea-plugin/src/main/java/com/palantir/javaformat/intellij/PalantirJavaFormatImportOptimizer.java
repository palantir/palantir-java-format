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

import com.google.common.util.concurrent.Runnables;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.FormatterService;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class PalantirJavaFormatImportOptimizer implements ImportOptimizer {

    private final Optional<FormatterService> formatterService;

    public PalantirJavaFormatImportOptimizer(Optional<FormatterService> formatterService) {
        this.formatterService = formatterService;
    }

    @Override
    public boolean supports(@NotNull PsiFile file) {
        return JavaFileType.INSTANCE.equals(file.getFileType())
                && PalantirJavaFormatSettings.getInstance(file.getProject()).isEnabled();
    }

    @Override
    public @NotNull Runnable processFile(@NotNull PsiFile file) {
        if (formatterService.isEmpty()) {
            Notifications.displayParsingErrorNotification(file.getProject(), file.getName());
            return Runnables.doNothing();
        }
        Project project = file.getProject();

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        Document document = documentManager.getDocument(file);

        if (document == null) {
            return Runnables.doNothing();
        }

        final String origText = document.getText();
        String text;
        try {
            text = formatterService.get().fixImports(origText);
        } catch (FormatterException e) {
            Notifications.displayParsingErrorNotification(project, file.getName());
            return Runnables.doNothing();
        }

        // pointless to change document text if it hasn't changed, plus this can interfere with
        // e.g. PalantirJavaFormattingService's output, i.e. it can overwrite the results from the main
        // formatter.
        if (text.equals(origText)) {
            return Runnables.doNothing();
        }

        return () -> {
            if (documentManager.isDocumentBlockedByPsi(document)) {
                documentManager.doPostponedOperationsAndUnblockDocument(document);
            }

            // similarly to above, don't overwrite new document text if it has changed - we use
            // getCharsSequence() as we should have `writeAction()` (which I think means effectively a
            // write-lock) and it saves calling getText(), which apparently is expensive.
            CharSequence newText = document.getCharsSequence();
            if (CharSequence.compare(origText, newText) != 0) {
                return;
            }

            document.setText(text);
        };
    }
}
