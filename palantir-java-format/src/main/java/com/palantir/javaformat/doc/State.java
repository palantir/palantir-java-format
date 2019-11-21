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

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.Indent;
import fj.data.Set;
import fj.data.TreeMap;
import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;

/** State for writing. */
@Value.Immutable
@Value.Style(overshadowImplementation = true)
@Immutable
public abstract class State {
    /** Last indent that was actually taken. */
    public abstract int lastIndent();
    /** Next indent, if the level is about to be broken. */
    public abstract int indent();

    public abstract int column();

    public abstract boolean mustBreak();
    /** Counts how many lines a particular formatting took. */
    public abstract int numLines();
    /**
     * Counts how many times reached a branch, where multiple formattings would be considered. Expected runtime is
     * exponential in this number.
     *
     * @see State#withNewBranch()
     */
    public abstract int branchingCoefficient();

    protected abstract Set<BreakTag> breakTagsTaken();

    protected abstract TreeMap<Break, BreakState> breakStates();

    protected abstract TreeMap<Level, LevelState> levelStates();

    /**
     * Keep track of how each {@link Comment} was written (these are mostly comments), which can differ depending on the
     * starting column and the maxLength.
     */
    protected abstract TreeMap<Comment, TokState> tokStates();

    public static State startingState() {
        return builder()
                .lastIndent(0)
                .indent(0)
                .column(0)
                .mustBreak(false)
                .numLines(0)
                .branchingCoefficient(0)
                .breakTagsTaken(Set.empty(HasUniqueId.ord()))
                .breakStates(TreeMap.empty(HasUniqueId.ord()))
                .levelStates(TreeMap.empty(HasUniqueId.ord()))
                .tokStates(TreeMap.empty(HasUniqueId.ord()))
                .build();
    }

    public BreakState getBreakState(Break brk) {
        return breakStates().get(brk).orSome(ImmutableBreakState.of(false, -1));
    }

    public boolean wasBreakTaken(BreakTag breakTag) {
        return breakTagsTaken().member(breakTag);
    }

    boolean isOneLine(Level level) {
        LevelState levelState = levelStates().get(level).toNull();
        return levelState != null && levelState.oneLine();
    }

    String getTokText(Comment comment) {
        return Preconditions.checkNotNull(
                        tokStates().get(comment).toNull(), "Expected Tok state to exist for: %s", comment)
                .text();
    }

    /** Record whether break was taken. */
    State breakTaken(BreakTag breakTag, boolean broken) {
        boolean currentlyBroken = breakTagsTaken().member(breakTag);
        // TODO(dsanduleac): is the opposite ever a valid state?
        if (currentlyBroken != broken) {
            Set<BreakTag> newSet;
            if (broken) {
                newSet = breakTagsTaken().insert(breakTag);
            } else {
                newSet = breakTagsTaken().delete(breakTag);
            }
            return builder().from(this).breakTagsTaken(newSet).build();
        }
        return this;
    }

    /**
     * Increases the indent by {@link Indent#eval} of the {@code plusIndent}, and sets {@code mustBreak} to false. Does
     * not commit to the indent just yet though, so lastIndent stays the same.
     */
    State withIndentIncrementedBy(Indent plusIndent) {
        return builder()
                .from(this)
                .indent(indent() + plusIndent.eval(this))
                .mustBreak(false)
                .build();
    }

    /** Reset any accumulated indent to the same value as {@code lastIndent}. */
    State withNoIndent() {
        return builder().from(this).indent(lastIndent()).mustBreak(false).build();
    }

    /** The current level is being broken and it has breaks in it. Commit to the indent. */
    State withBrokenLevel() {
        return builder().from(this).lastIndent(indent()).build();
    }

    State withBreak(Break brk, boolean broken) {
        Builder builder = builder().from(this);

        if (broken) {
            int newColumn = Math.max(indent() + brk.evalPlusIndent(this), 0);

            return builder
                    // lastIndent = indent -- we've proven that we wrote some stuff at the new 'indent'
                    .lastIndent(indent())
                    .column(newColumn)
                    .numLines(numLines() + 1)
                    .breakStates(breakStates().set(brk, ImmutableBreakState.of(true, newColumn)))
                    .build();
        } else {
            return builder.column(column() + brk.getFlat().length()).build();
        }
    }

    /** Update the current state after having processed an _inner_ level. */
    State updateAfterLevel(State afterInnerLevel) {
        return builder()
                // Inherited current state
                .lastIndent(lastIndent())
                .indent(indent())
                .branchingCoefficient(branchingCoefficient())
                .mustBreak(mustBreak())
                // Overridden state
                .column(afterInnerLevel.column())
                .numLines(afterInnerLevel.numLines())
                // TODO(dsanduleac): put these behind a "GlobalState"
                .breakTagsTaken(afterInnerLevel.breakTagsTaken())
                .breakStates(afterInnerLevel.breakStates())
                .levelStates(afterInnerLevel.levelStates())
                .tokStates(afterInnerLevel.tokStates())
                .build();
    }

    State addNewLines(int extraNewlines) {
        return builder().from(this).numLines(numLines() + extraNewlines).build();
    }

    State withColumn(int column) {
        return builder().from(this).column(column).build();
    }

    State withMustBreak(boolean mustBreak) {
        return builder().from(this).mustBreak(mustBreak).build();
    }

    State withNewBranch() {
        return builder()
                .from(this)
                .branchingCoefficient(branchingCoefficient() + 1)
                .build();
    }

    State withLevelState(Level level, LevelState levelState) {
        return builder()
                .from(this)
                .levelStates(levelStates().set(level, levelState))
                .build();
    }

    State withTokState(Comment comment, TokState tokState) {
        return builder()
                .from(this)
                .tokStates(tokStates().set(comment, tokState))
                .build();
    }

    public static class Builder extends ImmutableState.Builder {}

    public static Builder builder() {
        return new Builder();
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    interface BreakState {
        @Parameter
        boolean broken();

        @Parameter
        int newIndent();
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    interface LevelState {
        /** True if the entire {@link Level} fits on one line. */
        @Parameter
        boolean oneLine();
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    interface TokState {
        @Parameter
        String text();
    }
}
