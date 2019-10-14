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

import static java.util.Comparator.comparing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.util.IncorrectOperationException;
import com.palantir.javaformat.java.FormatterFactory;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CodeStyleManager} implementation which formats .java files with google-java-format. Formatting of all other
 * types of files is delegated to IJ's default implementation.
 */
class GoogleJavaFormatCodeStyleManager extends CodeStyleManagerDecorator {
    private static final Logger log = LoggerFactory.getLogger(GoogleJavaFormatCodeStyleManager.class);

    private static final String PLUGIN_ID = "palantir-java-format";

    public GoogleJavaFormatCodeStyleManager(@NotNull CodeStyleManager original) {
        super(original);
    }

    @Override
    public void reformatText(PsiFile file, int startOffset, int endOffset) throws IncorrectOperationException {
        if (overrideFormatterForFile(file)) {
            formatInternal(file, ImmutableList.of(new TextRange(startOffset, endOffset)));
        } else {
            super.reformatText(file, startOffset, endOffset);
        }
    }

    @Override
    public void reformatText(PsiFile file, Collection<TextRange> ranges) throws IncorrectOperationException {
        if (overrideFormatterForFile(file)) {
            formatInternal(file, ranges);
        } else {
            super.reformatText(file, ranges);
        }
    }

    @Override
    public void reformatTextWithContext(PsiFile file, Collection<TextRange> ranges) {
        if (overrideFormatterForFile(file)) {
            formatInternal(file, ranges);
        } else {
            super.reformatTextWithContext(file, ranges);
        }
    }

    @Override
    public PsiElement reformatRange(
            PsiElement element, int startOffset, int endOffset, boolean canChangeWhiteSpacesOnly) {
        // Only handle elements that are PsiFile for now -- otherwise we need to search for some
        // element within the file at new locations given the original startOffset and endOffsets
        // to serve as the return value.
        PsiFile file = element instanceof PsiFile ? (PsiFile) element : null;
        if (file != null && canChangeWhiteSpacesOnly && overrideFormatterForFile(file)) {
            formatInternal(file, ImmutableList.of(new TextRange(startOffset, endOffset)));
            return file;
        } else {
            return super.reformatRange(element, startOffset, endOffset, canChangeWhiteSpacesOnly);
        }
    }

    /** Return whether or not this formatter can handle formatting the given file. */
    private boolean overrideFormatterForFile(PsiFile file) {
        return StdFileTypes.JAVA.equals(file.getFileType())
                && PalantirJavaFormatSettings.getInstance(getProject()).isEnabled();
    }

    private void formatInternal(PsiFile file, Collection<TextRange> ranges) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
        documentManager.commitAllDocuments();
        CheckUtil.checkWritable(file);

        Document document = documentManager.getDocument(file);

        if (document == null) {
            return;
        }
        // If there are postponed PSI changes (e.g., during a refactoring), just abort.
        // If we apply them now, then the incoming text ranges may no longer be valid.
        if (documentManager.isDocumentBlockedByPsi(document)) {
            return;
        }

        format(document, ranges);
    }

    private static URL toUrlUnchecked(URI uri) {
        try {
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new RuntimeException("Couldn't convert URI to URL: " + uri, e);
        }
    }

    private static URL[] listDirAsUrlsUnchecked(Path dir) {
        try (Stream<Path> list = Files.list(dir)) {
            return list.map(Path::toUri).map(GoogleJavaFormatCodeStyleManager::toUrlUnchecked).toArray(URL[]::new);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't list dir: " + dir.toString(), e);
        }
    }

    /**
     * Format the ranges of the given document.
     *
     * <p>Overriding methods will need to modify the document with the result of the external formatter (usually using
     * {@link #performReplacements(Document, Map)}.
     */
    private void format(Document document, Collection<TextRange> ranges) {
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(getProject());
        Style style = settings.getStyle();

        IdeaPluginDescriptor plugin = Preconditions.checkNotNull(
                PluginManager.getPlugin(PluginId.getId(PLUGIN_ID)), "Couldn't find our own plugin: %s", PLUGIN_ID);

        URL[] implementationUrls = settings.getImplementationClassPath()
                .map(implementationUris -> {
                    log.debug("Using palantir-java-format implementation defined by URIs: {}", implementationUris);
                    return implementationUris.stream().map(GoogleJavaFormatCodeStyleManager::toUrlUnchecked).toArray(
                            URL[]::new);
                })
                .orElseGet(() -> {
                    // Load from the jars bundled with the plugin.
                    Path implDir = plugin.getPath().toPath().resolve("impl");
                    return listDirAsUrlsUnchecked(implDir);
                });
        URLClassLoader classLoader = new URLClassLoader(implementationUrls, plugin.getPluginClassLoader());

        FormatterFactory factory = ServiceLoader.load(FormatterFactory.class, classLoader).iterator().next();
        FormatterService formatter = factory.createFormatter(JavaFormatterOptions.builder().style(style).build());
        performReplacements(document, FormatterUtil.getReplacements(formatter, document.getText(), ranges));
    }

    private void performReplacements(final Document document, final Map<TextRange, String> replacements) {

        if (replacements.isEmpty()) {
            return;
        }

        TreeMap<TextRange, String> sorted = new TreeMap<>(comparing(TextRange::getStartOffset));
        sorted.putAll(replacements);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            for (Entry<TextRange, String> entry : sorted.descendingMap().entrySet()) {
                document.replaceString(
                        entry.getKey().getStartOffset(), entry.getKey().getEndOffset(), entry.getValue());
            }
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });
    }
}
