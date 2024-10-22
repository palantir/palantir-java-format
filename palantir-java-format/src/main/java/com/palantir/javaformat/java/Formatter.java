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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.FormattingError;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.OpsBuilder.OpsOutput;
import com.palantir.javaformat.Utils;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.DocBuilder;
import com.palantir.javaformat.doc.Level;
import com.palantir.javaformat.doc.NoopSink;
import com.palantir.javaformat.doc.Obs;
import com.palantir.javaformat.doc.Obs.Sink;
import com.palantir.javaformat.doc.State;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

/**
 * This is google-java-format, a new Java formatter that follows the Google Java Style Guide quite precisely---to the
 * letter and to the spirit.
 *
 * <p>This formatter uses the javac parser to generate an AST. Because the AST loses information about the non-tokens in
 * the input (including newlines, comments, etc.), and even some tokens (e.g., optional commas or semicolons), this
 * formatter lexes the input again and follows along in the resulting list of tokens. Its lexer splits all
 * multi-character operators (like "&gt;&gt;") into multiple single-character operators. Each non-token is assigned to a
 * token---non-tokens following a token on the same line go with that token; those following go with the next token---
 * and there is a final EOF token to hold final comments.
 *
 * <p>The formatter walks the AST to generate a Greg Nelson/Derek Oppen-style list of formatting {@link Op}s [1--2] that
 * then generates a structured {@link Doc}. Each AST node type has a visitor to emit a sequence of {@link Op}s for the
 * node.
 *
 * <p>Some data-structure operations are easier in the list of {@link Op}s, while others become easier in the
 * {@link Doc}. The {@link Op}s are walked to attach the comments. As the {@link Op}s are generated, missing input
 * tokens are inserted and incorrect output tokens are dropped, ensuring that the output matches the input even in the
 * face of formatter errors. Finally, the formatter walks the {@link Doc} to format it in the given width.
 *
 * <p>This formatter also produces data structures of which tokens and comments appear where on the input, and on the
 * output, to help output a partial reformatting of a slightly edited input.
 *
 * <p>Instances of the formatter are immutable and thread-safe.
 *
 * <p>[1] Nelson, Greg, and John DeTreville. Personal communication.
 *
 * <p>[2] Oppen, Derek C. "Prettyprinting". ACM Transactions on Programming Languages and Systems, Volume 2 Issue 4,
 * Oct. 1980, pp. 465–483.
 */
@Immutable
public final class Formatter {

    static final Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);

    private final JavaFormatterOptions options;
    private final boolean debugMode;

    @VisibleForTesting
    Formatter(JavaFormatterOptions options, boolean debugMode) {
        this.options = options;
        this.debugMode = debugMode;
    }

    /** A new Formatter instance with default options. */
    public static Formatter create() {
        return new Formatter(JavaFormatterOptions.defaultOptions(), false);
    }

    public static Formatter createFormatter(JavaFormatterOptions options) {
        return new Formatter(options, false);
    }

    /**
     * Construct a {@code Formatter} given a Java compilation unit. Parses the code; builds a {@link JavaInput} and the
     * corresponding {@link JavaOutput}.
     *
     * @param javaInput the input, a Java compilation unit
     * @param options the {@link JavaFormatterOptions}
     * @param commentsHelper the {@link CommentsHelper}, used to rewrite comments
     * @param debugMode whether to produce debugging output via {@link DebugRenderer}
     * @return javaOutput the output produced
     */
    static JavaOutput format(
            final JavaInput javaInput, JavaFormatterOptions options, CommentsHelper commentsHelper, boolean debugMode)
            throws FormatterException {

        Context context = new Context();
        Options.instance(context).put("allowStringFolding", "false");

        JCCompilationUnit unit = parseJcCompilationUnit(context, javaInput.getText());

        // Output the compilation unit.
        javaInput.setCompilationUnit(unit);
        OpsBuilder opsBuilder = new OpsBuilder(javaInput);

        JavaInputAstVisitor visitor;
        if (getRuntimeVersion() >= 21) {
            visitor = createVisitor("com.palantir.javaformat.java.java21.Java21InputAstVisitor", opsBuilder, options);
        } else if (getRuntimeVersion() >= 14) {
            visitor = createVisitor("com.palantir.javaformat.java.java14.Java14InputAstVisitor", opsBuilder, options);
        } else {
            visitor = new JavaInputAstVisitor(opsBuilder, options.indentationMultiplier());
        }

        visitor.scan(unit, null);
        opsBuilder.sync(javaInput.getText().length());
        opsBuilder.drain();
        OpsOutput opsOutput = opsBuilder.build();

        Level doc = new DocBuilder().withOps(opsOutput.ops()).build();

        // Don't even allocate all those JSON nodes if we're not going to write it out
        Sink sink = debugMode ? new JsonSink() : new NoopSink();

        Obs.ExplorationNode observationNode = Obs.createRoot(sink);
        State finalState =
                doc.computeBreaks(commentsHelper, options.maxLineLength(), State.startingState(), observationNode);

        JavaOutput javaOutput = new JavaOutput(javaInput, opsOutput.inputMetadata());
        doc.write(finalState, javaOutput);
        javaOutput.flush();

        if (debugMode) {
            DebugRenderer.render(javaInput, opsOutput, doc, finalState, javaOutput, sink.getOutput());
        }
        return javaOutput;
    }

    static JCCompilationUnit parseJcCompilationUnit(Context context, String sourceText) throws FormatterException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        context.put(DiagnosticListener.class, diagnostics);
        Options.instance(context).put("--enable-preview", "true");
        JCCompilationUnit unit;
        JavacFileManager fileManager = new JavacFileManager(context, true, UTF_8);
        try {
            fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, ImmutableList.of());
        } catch (IOException e) {
            // impossible
            throw new RuntimeException(e);
        }
        SimpleJavaFileObject source = new SimpleJavaFileObject(URI.create("source"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return sourceText;
            }
        };
        Log.instance(context).useSource(source);
        ParserFactory parserFactory = ParserFactory.instance(context);
        JavacParser parser = parserFactory.newParser(
                sourceText, /*keepDocComments=*/ true, /*keepEndPos=*/ true, /*keepLineMap=*/ true);
        unit = parser.parseCompilationUnit();
        unit.sourcefile = source;

        Iterable<Diagnostic<? extends JavaFileObject>> errorDiagnostics =
                Iterables.filter(diagnostics.getDiagnostics(), Formatter::errorDiagnostic);
        if (!Iterables.isEmpty(errorDiagnostics)) {
            throw FormatterExceptions.fromJavacDiagnostics(errorDiagnostics);
        }
        return unit;
    }

    @VisibleForTesting
    static int getRuntimeVersion() {
        return Runtime.version().feature();
    }

    private static JavaInputAstVisitor createVisitor(
            final String className, final OpsBuilder builder, final JavaFormatterOptions options) {
        try {
            return Class.forName(className)
                    .asSubclass(JavaInputAstVisitor.class)
                    .getConstructor(OpsBuilder.class, int.class)
                    .newInstance(builder, options.indentationMultiplier());
        } catch (ReflectiveOperationException e) {
            throw new LinkageError(e.getMessage(), e);
        }
    }

    static boolean errorDiagnostic(Diagnostic<?> input) {
        if (input.getKind() != Diagnostic.Kind.ERROR) {
            return false;
        }
        switch (input.getCode()) {
            case "compiler.err.invalid.meth.decl.ret.type.req":
                // accept constructor-like method declarations that don't match the name of their
                // enclosing class
                return false;
            default:
                break;
        }
        return true;
    }

    /**
     * Format the given input (a Java compilation unit) into the output stream.
     *
     * @throws FormatterException if the input cannot be parsed
     */
    public void formatSource(CharSource input, CharSink output) throws FormatterException, IOException {
        // TODO(cushon): proper support for streaming input/output. Input may
        // not be feasible (parsing) but output should be easier.
        output.write(formatSource(input.read()));
    }

    /**
     * Format an input string (a Java compilation unit) into an output string.
     *
     * <p>Leaves import statements untouched.
     *
     * @param input the input string
     * @return the output string
     * @throws FormatterException if the input string cannot be parsed
     */
    public String formatSource(String input) throws FormatterException {
        return formatSource(input, ImmutableList.of(Range.closedOpen(0, input.length())));
    }

    /**
     * Formats an input string (a Java compilation unit) and fixes imports.
     *
     * <p>Fixing imports includes ordering, spacing, and removal of unused import statements.
     *
     * @param input the input string
     * @return the output string
     * @throws FormatterException if the input string cannot be parsed
     * @see <a href="https://google.github.io/styleguide/javaguide.html#s3.3.3-import-ordering-and-spacing">Google Java
     *     Style Guide - 3.3.3 Import ordering and spacing</a>
     */
    public String formatSourceAndFixImports(String input) throws FormatterException {
        input = ImportOrderer.reorderImports(input, options.style());
        input = RemoveUnusedImports.removeUnusedImports(input);
        String formatted = formatSource(input);
        formatted = StringWrapper.wrap(options.maxLineLength(), formatted, this);
        return formatted;
    }

    /**
     * Fixes imports (e.g. ordering, spacing, and removal of unused import statements).
     *
     * @param input the input string
     * @return the output string
     * @throws FormatterException if the input string cannot be parsed
     * @see <a href="https://google.github.io/styleguide/javaguide.html#s3.3.3-import-ordering-and-spacing">Google Java
     *     Style Guide - 3.3.3 Import ordering and spacing</a>
     */
    public String fixImports(String input) throws FormatterException {
        return ImportOrderer.reorderImports(RemoveUnusedImports.removeUnusedImports(input), options.style());
    }

    /**
     * Format an input string (a Java compilation unit), for only the specified character ranges. These ranges are
     * extended as necessary (e.g., to encompass whole lines).
     *
     * @param input the input string
     * @param characterRanges the character ranges to be reformatted
     * @return the output string
     * @throws FormatterException if the input string cannot be parsed
     */
    public String formatSource(String input, Collection<Range<Integer>> characterRanges) throws FormatterException {
        return Utils.applyReplacements(input, getFormatReplacements(input, characterRanges));
    }

    /**
     * Emit a list of {@link Replacement}s to convert from input to output.
     *
     * @param input the input compilation unit
     * @param characterRanges the character ranges to reformat
     * @return a list of {@link Replacement}s, sorted from low index to high index, without overlaps
     * @throws FormatterException if the input string cannot be parsed
     */
    public ImmutableList<Replacement> getFormatReplacements(String input, Collection<Range<Integer>> characterRanges)
            throws FormatterException {
        JavaInput javaInput = new JavaInput(input);

        // TODO(cushon): this is only safe because the modifier ordering doesn't affect whitespace,
        // and doesn't change the replacements that are output. This is not true in general for
        // 'de-linting' changes (e.g. import ordering).
        javaInput = ModifierOrderer.reorderModifiers(javaInput, characterRanges);

        JavaCommentsHelper commentsHelper = new JavaCommentsHelper(javaInput.getLineSeparator(), options);
        JavaOutput javaOutput;
        try {
            javaOutput = format(javaInput, options, commentsHelper, debugMode);
        } catch (FormattingError e) {
            throw new FormatterException(e.diagnostics());
        }
        RangeSet<Integer> tokenRangeSet = javaInput.characterRangesToTokenRanges(characterRanges);
        return javaOutput.getFormatReplacements(tokenRangeSet);
    }
}
