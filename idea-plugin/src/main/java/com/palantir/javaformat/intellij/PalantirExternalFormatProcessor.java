package com.palantir.javaformat.intellij;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ExternalFormatProcessor;
import com.intellij.psi.impl.CheckUtil;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.FormatterService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PalantirExternalFormatProcessor implements ExternalFormatProcessor {
    private static final Logger log = LoggerFactory.getLogger(PalantirExternalFormatProcessor.class);
    private static final String PLUGIN_ID = "palantir-java-format";
    private static final IdeaPluginDescriptor PLUGIN = Preconditions.checkNotNull(
            PluginManager.getPlugin(PluginId.getId(PLUGIN_ID)), "Couldn't find our own plugin: %s", PLUGIN_ID);

    // Cache to avoid creating a URLClassloader every time we want to format from IntelliJ
    private final LoadingCache<Optional<List<URI>>, FormatterService> implementationCache =
            Caffeine.newBuilder().maximumSize(1).build(PalantirExternalFormatProcessor::createFormatter);

    static Map<TextRange, String> getReplacements(
            FormatterService formatter, String text, Collection<TextRange> ranges) {
        try {
            ImmutableMap.Builder<TextRange, String> replacements = ImmutableMap.builder();
            formatter.getFormatReplacements(text, toRanges(ranges)).forEach(replacement -> {
                replacements.put(toTextRange(replacement.getReplaceRange()), replacement.getReplacementString());
            });
            return replacements.build();
        } catch (FormatterException e) {
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

    private TextRange formatInternal(PsiFile file, TextRange range) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        Project project = file.getProject();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        documentManager.commitAllDocuments();
        CheckUtil.checkWritable(file);

        Document document = file.getViewProvider().getDocument();

        log.info("Formatting file: {}, document: {}", file, document);

        if (document == null) {
            return null;
        }
        // If there are postponed PSI changes (e.g., during a refactoring), just abort.
        // If we apply them now, then the incoming text ranges may no longer be valid.
        if (documentManager.isDocumentBlockedByPsi(document)) {
            return null;
        }

        boolean replacementsPerformed = format(project, document, range);

        return replacementsPerformed ? range : null;
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
            return list.map(Path::toUri).map(PalantirExternalFormatProcessor::toUrlUnchecked).toArray(URL[]::new);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't list dir: " + dir.toString(), e);
        }
    }

    /**
     * Format the ranges of the given document.
     *
     * <p>Overriding methods will need to modify the document with the result of the external formatter (usually using
     * {@link #performReplacements(Project, Document, Map)}.
     *
     * @return whether replacements were performed
     */
    private boolean format(Project project, Document document, TextRange range) {
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        FormatterService formatter =
                Objects.requireNonNull(implementationCache.get(settings.getImplementationClassPath()));

        Map<TextRange, String> replacements = getReplacements(formatter, document.getText(), ImmutableList.of(range));
        performReplacements(project, document, replacements);

        return !replacements.isEmpty();
    }

    private static FormatterService createFormatter(Optional<List<URI>> implementationClassPath) {
        URL[] implementationUrls = implementationClassPath
                .map(implementationUris -> {
                    log.debug("Using palantir-java-format implementation defined by URIs: {}", implementationUris);
                    return implementationUris.stream().map(PalantirExternalFormatProcessor::toUrlUnchecked).toArray(
                            URL[]::new);
                })
                .orElseGet(() -> {
                    // Load from the jars bundled with the plugin.
                    Path implDir = PLUGIN.getPath().toPath().resolve("impl");
                    log.debug("Using palantir-java-format implementation bundled with plugin: {}", implDir);
                    return listDirAsUrlsUnchecked(implDir);
                });
        ClassLoader classLoader = new URLClassLoader(implementationUrls, PLUGIN.getPluginClassLoader());
        return Iterables.getOnlyElement(ServiceLoader.load(FormatterService.class, classLoader));
    }

    private void performReplacements(Project project, final Document document, final Map<TextRange, String> replacements) {

        if (replacements.isEmpty()) {
            return;
        }

        TreeMap<TextRange, String> sorted = new TreeMap<>(comparing(TextRange::getStartOffset));
        sorted.putAll(replacements);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (Entry<TextRange, String> entry : sorted.descendingMap().entrySet()) {
                document.replaceString(
                        entry.getKey().getStartOffset(), entry.getKey().getEndOffset(), entry.getValue());
            }
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }

    @Override
    public boolean activeForFile(@NotNull PsiFile source) {
        return StdFileTypes.JAVA.equals(source.getFileType())
                && PalantirJavaFormatSettings.getInstance(source.getProject()).isEnabled();
    }

    @Nullable
    @Override
    public TextRange format(@NotNull PsiFile source, @NotNull TextRange range, boolean _canChangeWhiteSpacesOnly) {
        return formatInternal(source, range);
    }

    @NotNull
    @Override
    public String getId() {
        return "palantir-java-format.externalFormatProcessor";
    }
}
