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

import com.google.common.collect.Range;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.Output;
import com.palantir.javaformat.doc.State.BreakState;
import java.util.Optional;
import org.immutables.value.Value;

/** A leaf node in a {@link Doc} for an optional break. */
@Immutable
@Value.Immutable
public abstract class Break extends Doc implements Op {
    public abstract FillMode fillMode();

    public abstract String flat();

    public abstract Indent plusIndent();

    public abstract Optional<BreakTag> optTag();

    /**
     * Make a {@code Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the text when not broken
     * @param plusIndent extra indent if taken
     * @return the new {@code Break}
     */
    public static Break make(FillMode fillMode, String flat, Indent plusIndent) {
        return builder().fillMode(fillMode).flat(flat).plusIndent(plusIndent).build();
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
    public static Break make(FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optTag) {
        return builder()
                .fillMode(fillMode)
                .flat(flat)
                .plusIndent(plusIndent)
                .optTag(optTag)
                .build();
    }

    /**
     * Make a forced {@code Break}.
     *
     * @return the new forced {@code Break}
     */
    public static Break makeForced() {
        return builder()
                .fillMode(FillMode.FORCED)
                .flat("")
                .plusIndent(Indent.Const.ZERO)
                .build();
    }

    /**
     * Return the {@code Break}'s extra indent.
     *
     * @return the extra indent
     */
    public int evalPlusIndent(State state) {
        return plusIndent().eval(state);
    }

    /**
     * Is the {@code Break} forced?
     *
     * @return whether the {@code Break} is forced
     */
    public boolean isForced() {
        return fillMode() == FillMode.FORCED;
    }

    @Override
    public void add(DocBuilder builder) {
        builder.breakDoc(this);
    }

    @Override
    protected float computeWidth() {
        return isForced() ? Float.POSITIVE_INFINITY : (float) flat().length();
    }

    @Override
    protected String computeFlat() {
        return flat();
    }

    @Override
    protected Range<Integer> computeRange() {
        return EMPTY_RANGE;
    }

    public State computeBreaks(State stateIn, boolean broken) {
        State state = optTag().map(breakTag -> stateIn.breakTaken(breakTag, broken)).orElse(stateIn);
        return state.withBreak(this, broken);
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
    public void write(State state, Output output) {
        BreakState breakState = state.getBreakState(this);
        if (breakState.broken()) {
            output.append(state, "\n", EMPTY_RANGE);
            output.indent(breakState.newIndent());
        } else {
            output.append(state, flat(), range());
        }
    }

    public static class Builder extends ImmutableBreak.Builder {}

    public static Builder builder() {
        return new Builder();
    }
}
