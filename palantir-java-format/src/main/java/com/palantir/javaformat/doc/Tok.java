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
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Input;
import com.palantir.javaformat.Newlines;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.Output;

/** A leaf node in a {@link Doc} for a non-token. */
public final class Tok extends Doc implements Op {
    private final Input.Tok tok;
    String text;

    private Tok(Input.Tok tok) {
        this.tok = tok;
    }

    /**
     * Factory method for a {@code Tok}.
     *
     * @param tok the {@link Input.Tok} to wrap
     * @return the new {@code Tok}
     */
    public static Tok make(Input.Tok tok) {
        return new Tok(tok);
    }

    @Override
    public void add(DocBuilder builder) {
        builder.add(this);
    }

    @Override
    float computeWidth() {
        int idx = Newlines.firstBreak(tok.getOriginalText());
        // only count the first line of multi-line block comments
        if (tok.isComment()) {
            if (idx > 0) {
                return idx;
            } else if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
                // Account for line comments with missing spaces, see computeFlat.
                return tok.length() + 1;
            } else {
                return tok.length();
            }
        }
        return idx != -1 ? Float.POSITIVE_INFINITY : (float) tok.length();
    }

    @Override
    String computeFlat() {
        // TODO(cushon): commentsHelper.rewrite doesn't get called for spans that fit in a single
        // line. That's fine for multi-line comment reflowing, but problematic for adding missing
        // spaces in line comments.
        if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
            return "// " + tok.getOriginalText().substring("//".length());
        }
        return tok.getOriginalText();
    }

    @Override
    Range<Integer> computeRange() {
        return Range.singleton(tok.getIndex()).canonical(INTEGERS);
    }

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
        text = commentsHelper.rewrite(tok, maxWidth, state.column);
        int firstLineLength = text.length() - Iterators.getLast(Newlines.lineOffsetIterator(text));
        return state.withColumn(state.column + firstLineLength)
                .addNewLines(Iterators.size(Newlines.lineOffsetIterator(text)));
    }

    @Override
    public void write(Output output) {
        output.append(text, range());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("tok", tok).toString();
    }
}
