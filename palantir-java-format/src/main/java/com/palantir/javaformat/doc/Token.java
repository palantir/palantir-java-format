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

package com.palantir.javaformat.doc;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Range;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.Input;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.Output;
import java.util.Optional;

/** A leaf {@link Doc} for a token. */
@Immutable
public final class Token extends Doc implements Op {
    /** Is a Token a real token, or imaginary (e.g., a token generated incorrectly, or an EOF)? */
    public enum RealOrImaginary {
        REAL,
        IMAGINARY;
    }

    private final Input.Token token;
    private final RealOrImaginary realOrImaginary;
    private final Indent plusIndentCommentsBefore;
    private final Optional<Indent> breakAndIndentTrailingComment;

    private Token(
            Input.Token token,
            RealOrImaginary realOrImaginary,
            Indent plusIndentCommentsBefore,
            Optional<Indent> breakAndIndentTrailingComment) {
        this.token = token;
        this.realOrImaginary = realOrImaginary;
        this.plusIndentCommentsBefore = plusIndentCommentsBefore;
        this.breakAndIndentTrailingComment = breakAndIndentTrailingComment;
    }

    /**
     * How much extra to indent comments before the {@code Token}.
     *
     * @return the extra indent
     */
    public Indent getPlusIndentCommentsBefore() {
        return plusIndentCommentsBefore;
    }

    /** Force a line break and indent trailing javadoc or block comments. */
    public Optional<Indent> breakAndIndentTrailingComment() {
        return breakAndIndentTrailingComment;
    }

    /**
     * Make a {@code Token}.
     *
     * @param token the {@link Input.Token} to wrap
     * @param realOrImaginary did this {@link Input.Token} appear in the input, or was it generated incorrectly?
     * @param plusIndentCommentsBefore extra {@code plusIndent} for comments just before this token
     * @return the new {@code Token}
     */
    public static Token make(
            Input.Token token,
            RealOrImaginary realOrImaginary,
            Indent plusIndentCommentsBefore,
            Optional<Indent> breakAndIndentTrailingComment) {
        return new Token(token, realOrImaginary, plusIndentCommentsBefore, breakAndIndentTrailingComment);
    }

    /**
     * Return the wrapped {@link Input.Token}.
     *
     * @return the {@link Input.Token}
     */
    public Input.Token getToken() {
        return token;
    }

    /**
     * Is the token good? That is, does it match an {@link Input.Token}?
     *
     * @return whether the @code Token} is good
     */
    public RealOrImaginary realOrImaginary() {
        return realOrImaginary;
    }

    @Override
    public void add(DocBuilder builder) {
        builder.add(this);
    }

    @Override
    protected float computeWidth() {
        return token.getTok().length();
    }

    @Override
    protected String computeFlat() {
        return token.getTok().getOriginalText();
    }

    @Override
    protected Range<Integer> computeRange() {
        return Range.singleton(token.getTok().getIndex()).canonical(INTEGERS);
    }

    @Override
    public State computeBreaks(
            CommentsHelper commentsHelper, int maxWidth, State state, Obs.ExplorationNode observationNode) {
        String text = token.getTok().getOriginalText();
        return state.withColumn(state.column() + text.length());
    }

    @Override
    public void write(State state, Output output) {
        String text = token.getTok().getOriginalText();
        output.append(state, text, range());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("token", token)
                .add("realOrImaginary", realOrImaginary)
                .add("plusIndentCommentsBefore", plusIndentCommentsBefore)
                .toString();
    }
}
