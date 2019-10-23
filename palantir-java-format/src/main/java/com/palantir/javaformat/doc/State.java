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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.Output.BreakTag;
import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;

/** State for writing. */
@Value.Immutable
@Value.Style(overshadowImplementation = true)
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

    protected abstract ImmutableSet<BreakTag> breakTagsTaken();

    protected abstract ImmutableMap<Break, BreakState> breakStates();

    protected abstract ImmutableMap<Level, LevelState> levelStates();

    protected abstract ImmutableMap<Tok, TokState> tokStates();

    public static State startingState() {
        return builder()
                .lastIndent(0)
                .indent(0)
                .column(0)
                .mustBreak(false)
                .numLines(0)
                .branchingCoefficient(0)
                .breakTagsTaken(ImmutableSet.of())
                .breakStates(ImmutableMap.of())
                .levelStates(ImmutableMap.of())
                .tokStates(ImmutableMap.of())
                .build();
    }

    public BreakState getBreakState(Break brk) {
        return breakStates().getOrDefault(brk, ImmutableBreakState.of(false, -1));
    }

    public boolean wasBreakTaken(BreakTag breakTag) {
        return breakTagsTaken().contains(breakTag);
    }

    boolean isOneLine(Level level) {
        LevelState levelState = levelStates().get(level);
        return levelState != null && levelState.oneLine();
    }

    String getTokText(Tok tok) {
        return Preconditions.checkNotNull(tokStates().get(tok), "Expected Tok state to exist for: %s", tok).text();
    }

    /** Record whether break was taken. */
    State breakTaken(BreakTag breakTag, boolean broken) {
        boolean currentlyBroken = breakTagsTaken().contains(breakTag);
        // TODO(dsanduleac): is the opposite ever a valid state?
        if (currentlyBroken != broken) {
            if (broken) {
                return builder().from(this).addBreakTagsTaken(breakTag).build();
            } else {
                return builder()
                        .from(this)
                        .breakTagsTaken(breakTagsTaken().stream().filter(it -> it != breakTag).collect(
                                ImmutableSet.toImmutableSet()))
                        .build();
            }
        }
        return this;
    }

    /**
     * Increases the indent by {@link Indent#eval} of the {@code plusIndent}, and sets {@code mustBreak} to false. Does
     * not commit to the indent just yet though, so lastIndent stays the same.
     */
    State withIndentIncrementedBy(Indent plusIndent) {
        return builder().from(this).indent(indent() + plusIndent.eval(this)).mustBreak(false).build();
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
                    .putBreakStates(brk, ImmutableBreakState.of(true, newColumn))
                    .build();
        } else {
            return builder.column(column() + brk.getFlat().length()).build();
        }
    }

    State updateAfterLevel(State state) {
        return builder()
                .from(this)
                .column(state.column())
                .numLines(state.numLines())
                // TODO(dsanduleac): put these behind a "GlobalState"
                .breakTagsTaken(state.breakTagsTaken())
                .breakStates(state.breakStates())
                .levelStates(state.levelStates())
                .tokStates(state.tokStates())
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
        return builder().from(this).branchingCoefficient(branchingCoefficient() + 1).build();
    }

    State withLevelState(Level level, LevelState levelState) {
        return builder().from(this).putLevelStates(level, levelState).build();
    }

    State withTokState(Tok tok, TokState tokState) {
        return builder().from(this).putTokStates(tok, tokState).build();
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
