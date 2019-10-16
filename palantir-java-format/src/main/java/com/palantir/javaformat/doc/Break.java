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
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.Output;
import com.palantir.javaformat.Output.BreakTag;
import java.util.Optional;

/** A leaf node in a {@link Doc} for an optional break. */
public final class Break extends Doc implements Op {
    private final FillMode fillMode;
    private final String flat;
    private final Indent plusIndent;
    private final Optional<Output.BreakTag> optTag;

    private boolean broken;
    private int newIndent;

    private Break(FillMode fillMode, String flat, Indent plusIndent, Optional<Output.BreakTag> optTag) {
        this.fillMode = fillMode;
        this.flat = flat;
        this.plusIndent = plusIndent;
        this.optTag = optTag;
    }

    public FillMode getFillMode() {
        return fillMode;
    }

    /**
     * Make a {@code Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the text when not broken
     * @param plusIndent extra indent if taken
     * @return the new {@code Break}
     */
    public static Break make(FillMode fillMode, String flat, Indent plusIndent) {
        return new Break(fillMode, flat, plusIndent, /* optTag= */ Optional.empty());
    }

    /**
     * Make a {@code Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the text when not broken
     * @param plusIndent extra indent if taken
     * @param optTag an optional tag for remembering whether the break was taken
     * @return the new {@code Break}
     */
    public static Break make(FillMode fillMode, String flat, Indent plusIndent, Optional<Output.BreakTag> optTag) {
        return new Break(fillMode, flat, plusIndent, optTag);
    }

    /**
     * Make a forced {@code Break}.
     *
     * @return the new forced {@code Break}
     */
    public static Break makeForced() {
        return make(FillMode.FORCED, "", Indent.Const.ZERO);
    }

    /**
     * Return the {@code Break}'s extra indent.
     *
     * @return the extra indent
     */
    public int evalPlusIndent() {
        return plusIndent.eval();
    }

    Indent getPlusIndent() {
        return plusIndent;
    }

    /**
     * Is the {@code Break} forced?
     *
     * @return whether the {@code Break} is forced
     */
    public boolean isForced() {
        return fillMode == FillMode.FORCED;
    }

    @Override
    public void add(DocBuilder builder) {
        builder.breakDoc(this);
    }

    @Override
    float computeWidth() {
        return isForced() ? Float.POSITIVE_INFINITY : (float) flat.length();
    }

    @Override
    String computeFlat() {
        return flat;
    }

    @Override
    Range<Integer> computeRange() {
        return EMPTY_RANGE;
    }

    /**
     * For use only by {@link ClearBreaksVisitor}. Use case: ensure fresh state when we decide a level should be
     * oneLine, because its inner breaks might have been previously broken in a different branch.
     */
    void clearBreak() {
        optTag.ifPresent(BreakTag::reset);
    }

    /** For use only by {@link LevelDelimitedFlatValueDocVisitor}. */
    public Optional<BreakTag> getOptTag() {
        return optTag;
    }

    public State computeBreaks(State state, boolean broken) {
        if (optTag.isPresent()) {
            optTag.get().recordBroken(broken);
        }

        if (broken) {
            this.broken = true;
            State newState = state.withBreak(this);
            this.newIndent = newState.column;
            return newState;
        } else {
            this.broken = false;
            this.newIndent = -1;
            return state.withColumn(state.column + flat.length());
        }
    }

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
        // Updating the state for {@link Break}s requires deciding if the break
        // should be taken.
        // TODO(cushon): this hierarchy is wrong, create a separate interface
        // for unbreakable Docs?
        throw new UnsupportedOperationException("Did you mean computeBreaks(State, int, boolean)?");
    }

    @Override
    public void write(Output output) {
        if (broken) {
            output.append("\n", EMPTY_RANGE);
            output.indent(newIndent);
        } else {
            output.append(flat, range());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fillMode", fillMode)
                .add("flat", flat)
                .add("plusIndent", plusIndent)
                .add("optTag", optTag)
                .toString();
    }
}
