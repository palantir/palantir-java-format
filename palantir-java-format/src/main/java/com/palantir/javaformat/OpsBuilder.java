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

package com.palantir.javaformat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.palantir.javaformat.Indent.Const;
import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.BreakTag;
import com.palantir.javaformat.doc.Comment;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.DocBuilder;
import com.palantir.javaformat.doc.FillMode;
import com.palantir.javaformat.doc.NonBreakingSpace;
import com.palantir.javaformat.doc.State;
import com.palantir.javaformat.doc.Token;
import com.palantir.javaformat.java.FormatterDiagnostic;
import com.palantir.javaformat.java.InputMetadata;
import com.palantir.javaformat.java.InputMetadataBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/** An {@code OpsBuilder} creates a list of {@link Op}s, which is turned into a {@link Doc} by {@link DocBuilder}. */
public final class OpsBuilder {

    /** @return the actual size of the AST node at position, including comments. */
    public int actualSize(int position, int length) {
        Input.Token startToken = input.getPositionTokenMap().get(position);
        int start = startToken.getTok().getPosition();
        for (Input.Tok tok : startToken.getToksBefore()) {
            if (tok.isComment()) {
                start = Math.min(start, tok.getPosition());
            }
        }
        Input.Token endToken = input.getPositionTokenMap().get(position + length - 1);
        int end = endToken.getTok().getPosition() + endToken.getTok().length();
        for (Input.Tok tok : endToken.getToksAfter()) {
            if (tok.isComment()) {
                end = Math.max(end, tok.getPosition() + tok.length());
            }
        }
        return end - start;
    }

    /** @return the start column of the token at {@code position}, including leading comments. */
    public Integer actualStartColumn(int position) {
        Input.Token startToken = input.getPositionTokenMap().get(position);
        int start = startToken.getTok().getPosition();
        int line0 = input.getLineNumber(start);
        for (Input.Tok tok : startToken.getToksBefore()) {
            if (line0 != input.getLineNumber(tok.getPosition())) {
                return start;
            }
            if (tok.isComment()) {
                start = Math.min(start, tok.getPosition());
            }
        }
        return start;
    }

    /** A request to add or remove a blank line in the output. */
    public abstract static class BlankLineWanted {

        /** Always emit a blank line. */
        public static final BlankLineWanted YES = new SimpleBlankLine(Optional.of(true));

        /** Never emit a blank line. */
        public static final BlankLineWanted NO = new SimpleBlankLine(Optional.of(false));

        /**
         * Explicitly preserve blank lines from the input (e.g. before the first member in a class declaration).
         * Overrides conditional blank lines.
         */
        public static final BlankLineWanted PRESERVE = new SimpleBlankLine(/* wanted= */ Optional.empty());

        /** Is the blank line wanted? */
        public abstract Optional<Boolean> wanted(State state);

        /** Merge this blank line request with another. */
        public abstract BlankLineWanted merge(BlankLineWanted wanted);

        /** Emit a blank line if the given break is taken. */
        public static BlankLineWanted conditional(BreakTag breakTag) {
            return new ConditionalBlankLine(ImmutableList.of(breakTag));
        }

        private static final class SimpleBlankLine extends BlankLineWanted {
            private final Optional<Boolean> wanted;

            SimpleBlankLine(Optional<Boolean> wanted) {
                this.wanted = wanted;
            }

            @Override
            public Optional<Boolean> wanted(State _state) {
                return wanted;
            }

            @Override
            public BlankLineWanted merge(BlankLineWanted other) {
                return this;
            }
        }

        private static final class ConditionalBlankLine extends BlankLineWanted {

            private final ImmutableList<BreakTag> tags;

            ConditionalBlankLine(Iterable<BreakTag> tags) {
                this.tags = ImmutableList.copyOf(tags);
            }

            @Override
            public Optional<Boolean> wanted(State state) {
                for (BreakTag tag : tags) {
                    if (state.wasBreakTaken(tag)) {
                        return Optional.of(true);
                    }
                }
                return Optional.empty();
            }

            @Override
            public BlankLineWanted merge(BlankLineWanted other) {
                if (!(other instanceof ConditionalBlankLine)) {
                    return other;
                }
                return new ConditionalBlankLine(Iterables.concat(this.tags, ((ConditionalBlankLine) other).tags));
            }
        }
    }

    private final Input input;
    private final List<Op> ops = new ArrayList<>();
    /** Used to record blank-line information. */
    private final InputMetadataBuilder inputMetadataBuilder = new InputMetadataBuilder();

    private static final Const ZERO = Const.ZERO;

    private int tokenI = 0;
    private int inputPosition = Integer.MIN_VALUE;

    /** The number of unclosed open ops in the input stream. */
    int depth = 0;

    /** Add an {@link Op}, and record open/close ops for later validation of unclosed levels. */
    private void add(Op op) {
        if (op instanceof OpenOp) {
            depth++;
        } else if (op instanceof CloseOp) {
            depth--;
            if (depth < 0) {
                throw new IllegalStateException();
            }
        }
        ops.add(op);
    }

    /** Add a list of {@link Op}s. */
    public void addAll(List<Op> ops) {
        for (Op op : ops) {
            add(op);
        }
    }

    /**
     * The {@code OpsBuilder} constructor.
     *
     * @param input the {@link Input}, used for retrieve information from the AST
     */
    public OpsBuilder(Input input) {
        this.input = input;
    }

    /** Get the {@code OpsBuilder}'s {@link Input}. */
    public Input getInput() {
        return input;
    }

    /** Returns the number of unclosed open ops in the input stream. */
    public int depth() {
        return depth;
    }

    /**
     * Checks that all open ops in the op stream have matching close ops.
     *
     * @throws FormattingError if any ops were unclosed
     */
    public void checkClosed(int previous) {
        if (depth != previous) {
            throw new FormattingError(diagnostic(String.format("saw %d unclosed ops", depth)));
        }
    }

    /** Create a {@link FormatterDiagnostic} at the current position. */
    public FormatterDiagnostic diagnostic(String message) {
        return input.createDiagnostic(inputPosition, message);
    }

    /**
     * Sync to position in the input. If we've skipped outputting any tokens that were present in the input tokens,
     * output them here and optionally complain.
     *
     * @param inputPosition the {@code 0}-based input position
     */
    public void sync(int inputPosition) {
        if (inputPosition > this.inputPosition) {
            ImmutableList<? extends Input.Token> tokens = input.getTokens();
            int tokensN = tokens.size();
            this.inputPosition = inputPosition;
            if (tokenI < tokensN && inputPosition > tokens.get(tokenI).getTok().getPosition()) {
                // Found a missing input token. Insert it and mark it missing (usually not good).
                Input.Token token = tokens.get(tokenI++);
                throw new FormattingError(
                        diagnostic(String.format("did not generate token \"%s\"", token.getTok().getText())));
            }
        }
    }

    /** Output any remaining tokens from the input stream (e.g. terminal whitespace). */
    public void drain() {
        int inputPosition = input.getText().length() + 1;
        if (inputPosition > this.inputPosition) {
            ImmutableList<? extends Input.Token> tokens = input.getTokens();
            int tokensN = tokens.size();
            while (tokenI < tokensN && inputPosition > tokens.get(tokenI).getTok().getPosition()) {
                Input.Token token = tokens.get(tokenI++);
                add(Token.make(
                        token,
                        Token.RealOrImaginary.IMAGINARY,
                        ZERO,
                        /* breakAndIndentTrailingComment= */ Optional.empty()));
            }
        }
        this.inputPosition = inputPosition;
        checkClosed(0);
    }

    /**
     * Open a new level by emitting an {@link OpenOp}.
     *
     * @param plusIndent the extra indent for the new level
     */
    public void open(Indent plusIndent) {
        add(OpenOp.make(plusIndent));
    }

    /**
     * Open a new level by emitting an {@link OpenOp}.
     *
     * @param debugName a representative name for this lambda
     * @param plusIndent the extra indent for the new level
     */
    public void open(String debugName, Indent plusIndent) {
        add(OpenOp.builder().plusIndent(plusIndent).debugName(debugName).build());
    }

    /**
     * Open a new level by emitting an {@link OpenOp}.
     *
     * @param plusIndent the extra indent for the new level
     * @param breakBehaviour how to decide whether to break this level or not
     * @param breakabilityIfLastLevel if last level, when to break this rather than parent
     */
    public void open(Indent plusIndent, BreakBehaviour breakBehaviour, LastLevelBreakability breakabilityIfLastLevel) {
        add(OpenOp.builder()
                .plusIndent(plusIndent)
                .breakBehaviour(breakBehaviour)
                .breakabilityIfLastLevel(breakabilityIfLastLevel)
                .build());
    }

    public void open(OpenOp openOp) {
        add(openOp);
    }

    /** Close the current level, by emitting a {@link CloseOp}. */
    public void close() {
        add(CloseOp.make());
    }

    /** Return the text of the next {@link Input.Token}, or absent if there is none. */
    public Optional<String> peekToken() {
        return peekToken(0);
    }

    /** Return the text of an upcoming {@link Input.Token}, or absent if there is none. */
    public Optional<String> peekToken(int skip) {
        ImmutableList<? extends Input.Token> tokens = input.getTokens();
        int idx = tokenI + skip;
        return idx < tokens.size() ? Optional.of(tokens.get(idx).getTok().getOriginalText()) : Optional.empty();
    }

    /**
     * Emit an optional token iff it exists on the input. This is used to emit tokens whose existence has been lost in
     * the AST.
     *
     * @param token the optional token
     */
    public void guessToken(String token) {
        token(token, Token.RealOrImaginary.IMAGINARY, ZERO, /* breakAndIndentTrailingComment=  */ Optional.empty());
    }

    public void token(
            String token,
            Token.RealOrImaginary realOrImaginary,
            Indent plusIndentCommentsBefore,
            Optional<Indent> breakAndIndentTrailingComment) {
        ImmutableList<? extends Input.Token> tokens = input.getTokens();
        if (token.equals(peekToken().orElse(null))) { // Found the input token. Output it.
            add(Token.make(
                    tokens.get(tokenI++),
                    Token.RealOrImaginary.REAL,
                    plusIndentCommentsBefore,
                    breakAndIndentTrailingComment));
        } else {
            /*
             * Generated a "bad" token, which doesn't exist on the input. Drop it, and complain unless
             * (for example) we're guessing at an optional token.
             */
            if (realOrImaginary == Token.RealOrImaginary.REAL) {
                throw new FormattingError(diagnostic(String.format(
                        "expected token: '%s'; generated %s instead", peekToken().orElse(null), token)));
            }
        }
    }

    /**
     * Emit a single- or multi-character op by breaking it into single-character {@link Token}s.
     *
     * @param op the operator to emit
     */
    public void op(String op) {
        int opN = op.length();
        for (int i = 0; i < opN; i++) {
            token(
                    op.substring(i, i + 1),
                    Token.RealOrImaginary.REAL,
                    ZERO,
                    /* breakAndIndentTrailingComment=  */ Optional.empty());
        }
    }

    /** Emit a {@link NonBreakingSpace}. */
    public void space() {
        add(NonBreakingSpace.make());
    }

    /** Emit a {@link Break}. */
    public void breakOp() {
        breakOp(FillMode.UNIFIED, "", ZERO);
    }

    /**
     * Emit a {@link Break}.
     *
     * @param plusIndent extra indent if taken
     */
    public void breakOp(Indent plusIndent) {
        breakOp(FillMode.UNIFIED, "", plusIndent);
    }

    /** Emit a filled {@link Break}. */
    public void breakToFill() {
        breakOp(FillMode.INDEPENDENT, "", ZERO);
    }

    /** Emit a forced {@link Break}. */
    public void forcedBreak() {
        breakOp(FillMode.FORCED, "", ZERO);
    }

    /**
     * Emit a forced {@link Break}.
     *
     * @param plusIndent extra indent if taken
     */
    public void forcedBreak(Indent plusIndent) {
        breakOp(FillMode.FORCED, "", plusIndent);
    }

    /**
     * Emit a {@link Break}, with a specified {@code flat} value (e.g., {@code " "}).
     *
     * @param flat the {@link Break} when not broken
     */
    public void breakOp(String flat) {
        breakOp(FillMode.UNIFIED, flat, ZERO);
    }

    /**
     * Emit a {@link Break}, with a specified {@code flat} value (e.g., {@code " "}).
     *
     * @param flat the {@link Break} when not broken
     */
    public void breakToFill(String flat) {
        breakOp(FillMode.INDEPENDENT, flat, ZERO);
    }

    /**
     * Emit a generic {@link Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the {@link Break} when not broken
     * @param plusIndent extra indent if taken
     */
    public void breakOp(FillMode fillMode, String flat, Indent plusIndent) {
        breakOp(fillMode, flat, plusIndent, /* optionalTag=  */ Optional.empty());
    }

    /**
     * Emit a generic {@link Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the {@link Break} when not broken
     * @param plusIndent extra indent if taken
     * @param optionalTag an optional tag for remembering whether the break was taken
     */
    public void breakOp(FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optionalTag) {
        add(Break.make(fillMode, flat, plusIndent, optionalTag));
    }

    private int lastPartialFormatBoundary = -1;

    /**
     * Make the boundary of a region that can be partially formatted. The boundary will be included in the following
     * region, e.g.: [[boundary0, boundary1), [boundary1, boundary2), ...].
     */
    public void markForPartialFormat() {
        if (lastPartialFormatBoundary == -1) {
            lastPartialFormatBoundary = tokenI;
            return;
        }
        if (tokenI == lastPartialFormatBoundary) {
            return;
        }
        Input.Token start = input.getTokens().get(lastPartialFormatBoundary);
        Input.Token end = input.getTokens().get(tokenI - 1);
        inputMetadataBuilder.markForPartialFormat(start, end);
        lastPartialFormatBoundary = tokenI;
    }

    /**
     * Force or suppress a blank line here in the output.
     *
     * @param wanted whether to force ({@code true}) or suppress {@code false}) the blank line
     */
    public void blankLineWanted(BlankLineWanted wanted) {
        inputMetadataBuilder.blankLine(getI(input.getTokens().get(tokenI)), wanted);
    }

    private static int getI(Input.Token token) {
        for (Input.Tok tok : token.getToksBefore()) {
            if (tok.getIndex() >= 0) {
                return tok.getIndex();
            }
        }
        return token.getTok().getIndex();
    }

    private static final NonBreakingSpace SPACE = NonBreakingSpace.make();

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    public interface OpsOutput {
        ImmutableList<Op> ops();

        InputMetadata inputMetadata();
    }

    /** Build a list of {@link Op}s from the {@code OpsBuilder}. */
    public OpsOutput build() {
        markForPartialFormat();
        // Rewrite the ops to insert comments.
        Multimap<Integer, Op> tokOps = ArrayListMultimap.create();
        int opsN = ops.size();
        for (int i = 0; i < opsN; i++) {
            Op op = ops.get(i);
            if (op instanceof Token) {
                /*
                 * Token ops can have associated non-tokens, including comments, which we need to insert.
                 * They can also cause line breaks, so we insert them before or after the current level,
                 * when possible.
                 */
                Token tokenOp = (Token) op;
                Input.Token token = tokenOp.getToken();
                int j = i; // Where to insert toksBefore before.
                while (0 < j && ops.get(j - 1) instanceof OpenOp) {
                    --j;
                }
                int k = i; // Where to insert toksAfter after.
                while (k + 1 < opsN && ops.get(k + 1) instanceof CloseOp) {
                    ++k;
                }
                if (tokenOp.realOrImaginary() == Token.RealOrImaginary.REAL) {
                    /*
                     * Regular input token. Copy out toksBefore before token, and toksAfter after it. Insert
                     * this token's toksBefore at position j.
                     */
                    int newlines = 0; // Count of newlines in a row.
                    boolean space = false; // Do we need an extra space after a previous "/*" comment?
                    boolean lastWasComment = false; // Was the last thing we output a comment?
                    boolean allowBlankAfterLastComment = false;
                    for (Input.Tok tokBefore : token.getToksBefore()) {
                        if (tokBefore.isNewline()) {
                            newlines++;
                        } else if (tokBefore.isComment()) {
                            tokOps.put(j, Break.make(
                                    tokBefore.isSlashSlashComment() ? FillMode.FORCED : FillMode.UNIFIED,
                                    "",
                                    tokenOp.getPlusIndentCommentsBefore()));
                            tokOps.putAll(j, makeComment(tokBefore));
                            space = tokBefore.isSlashStarComment();
                            newlines = 0;
                            lastWasComment = true;
                            if (tokBefore.isJavadocComment()) {
                                tokOps.put(j, Break.makeForced());
                            }
                            allowBlankAfterLastComment = tokBefore.isSlashSlashComment()
                                    || (tokBefore.isSlashStarComment() && !tokBefore.isJavadocComment());
                        }
                    }
                    if (allowBlankAfterLastComment && newlines > 1) {
                        // Force a line break after two newlines in a row following a line or block comment
                        inputMetadataBuilder.blankLine(token.getTok().getIndex(), BlankLineWanted.YES);
                    }
                    if (lastWasComment && newlines > 0) {
                        tokOps.put(j, Break.makeForced());
                    } else if (space) {
                        tokOps.put(j, SPACE);
                    }
                    // Now we've seen the Token; output the toksAfter.

                    // Reordering of NON-NLS comments that might follow a `+` in a chain of string concatenations, in
                    // order to move the comments before the Break that precedes the `+` token.
                    boolean nonNlsCommentsAfterPlus = token.getToksAfter().stream()
                                    .anyMatch(OpsBuilder::isNonNlsComment)
                            && token.getTok().getText().equals("+")
                            && k > 0
                            && ops.get(k - 1) instanceof Break;

                    int tokAfterPos = nonNlsCommentsAfterPlus ? k - 1 : k + 1;

                    for (Input.Tok tokAfter : token.getToksAfter()) {
                        if (tokAfter.isComment()) {
                            boolean breakAfter = tokAfter.isJavadocComment()
                                    || (tokAfter.isSlashStarComment()
                                            && tokenOp.breakAndIndentTrailingComment().isPresent());
                            if (breakAfter) {
                                tokOps.put(tokAfterPos, Break.make(
                                        FillMode.FORCED,
                                        "",
                                        tokenOp.breakAndIndentTrailingComment().orElse(Const.ZERO)));
                            } else {
                                tokOps.put(tokAfterPos, SPACE);
                            }
                            tokOps.putAll(tokAfterPos, makeComment(tokAfter));
                            if (breakAfter) {
                                tokOps.put(tokAfterPos, Break.make(FillMode.FORCED, "", ZERO));
                            }
                        }
                    }
                } else {
                    /*
                     * This input token was mistakenly not generated for output. As no whitespace or comments
                     * were generated (presumably), copy all input non-tokens literally, even spaces and
                     * newlines.
                     */
                    int newlines = 0;
                    boolean lastWasComment = false;
                    for (Input.Tok tokBefore : token.getToksBefore()) {
                        if (tokBefore.isNewline()) {
                            newlines++;
                        } else if (tokBefore.isComment()) {
                            newlines = 0;
                            lastWasComment = tokBefore.isComment();
                        }
                        if (lastWasComment && newlines > 0) {
                            tokOps.put(j, Break.makeForced());
                        }
                        tokOps.put(j, Comment.make(tokBefore));
                    }
                    for (Input.Tok tokAfter : token.getToksAfter()) {
                        tokOps.put(k + 1, Comment.make(tokAfter));
                    }
                }
            }
        }
        /*
         * Construct new list of ops, splicing in the comments. If a comment is inserted immediately
         * before a space, suppress the space.
         */
        ImmutableList.Builder<Op> newOps = ImmutableList.builder();
        boolean afterForcedBreak = false; // Was the last Op a forced break? If so, suppress spaces.
        for (int i = 0; i < opsN; i++) {
            for (Op op : tokOps.get(i)) {
                if (!(afterForcedBreak && op instanceof NonBreakingSpace)) {
                    newOps.add(op);
                    afterForcedBreak = isForcedBreak(op);
                }
            }
            Op op = ops.get(i);
            if (afterForcedBreak
                    && (op instanceof NonBreakingSpace
                            || (op instanceof Break
                                    && ((Break) op).evalPlusIndent(State.startingState()) == 0
                                    && " ".equals(((Doc) op).getFlat())))) {
                continue;
            }
            newOps.add(op);
            if (!(op instanceof OpenOp)) {
                afterForcedBreak = isForcedBreak(op);
            }
        }
        for (Op op : tokOps.get(opsN)) {
            if (!(afterForcedBreak && op instanceof NonBreakingSpace)) {
                newOps.add(op);
                afterForcedBreak = isForcedBreak(op);
            }
        }
        return ImmutableOpsOutput.builder().ops(newOps.build()).inputMetadata(inputMetadataBuilder.build()).build();
    }

    private static boolean isNonNlsComment(Input.Tok tokAfter) {
        return tokAfter.isSlashSlashComment() && tokAfter.getText().contains("$NON-NLS");
    }

    private static boolean isForcedBreak(Op op) {
        return op instanceof Break && ((Break) op).isForced();
    }

    private static List<Op> makeComment(Input.Tok comment) {
        return comment.isSlashStarComment()
                ? ImmutableList.of(Comment.make(comment))
                : ImmutableList.of(Comment.make(comment), Break.makeForced());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("input", input)
                .add("ops", ops)
                .add("tokenI", tokenI)
                .add("inputPosition", inputPosition)
                .toString();
    }
}
