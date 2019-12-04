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

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;

/**
 * These classes exist purely for observing the operation of {@link Doc#computeBreaks}, including all the alternative
 * hypotheses it has considered and rejected before deciding on the final formatting.
 */
public final class Obs {
    private Obs() {}

    public interface FinishLevelNode {
        void finishNode(int acceptedExplorationId);
    }

    public interface FinishExplorationNode {
        /** Indicate that the exploration node was successful and produced this {@code newState} */
        void finishNode(Level parentLevel, State newState);
    }

    /** Within an 'exploration', allows you to descend into child levels. */
    public interface ExplorationNode {
        LevelNode newChildNode(Level level, State state);

        int id();
    }

    public interface Sink {
        FinishExplorationNode startExplorationNode(
                int exporationId,
                OptionalInt parentLevelId,
                String humanDescription,
                int startColumn,
                Optional<State> incomingState);

        /**
         * @param levelNodeId the unique ID of the {@link LevelNode}. There can be multiple LevelNodes per {@link
         *     Level}.
         * @param parentExplorationId what exploration is this {@link LevelNode} a part of
         */
        @CheckReturnValue
        FinishLevelNode writeLevelNode(int levelNodeId, int parentExplorationId, State incomingState, Level level);

        String getOutput();
    }

    public static ExplorationNode createRoot(Sink sink) {
        return new ExplorationNodeImpl(null, "(initial node)", sink, 0, Optional.empty());
    }

    /** At a single level, you can explore various options for how to break lines and then accept one. */
    interface LevelNode {

        Exploration explore(String humanDescription, State incomingState, Function<ExplorationNode, State> supplier);

        int id();

        Optional<Exploration> maybeExplore(
                String humanDescription, State incomingState, Function<ExplorationNode, Optional<State>> supplier);

        State finishLevel(State state);
    }

    /** A handle that lets you accept exactly one 'exploration' of how to format a level. */
    interface Exploration {
        State markAccepted();

        State state();
    }

    private static class LevelNodeImpl extends HasUniqueId implements LevelNode {
        private final Level level;
        private final Sink sink;
        private final FinishLevelNode finisher;
        private final int startColumn;

        public LevelNodeImpl(Level level, State incomingState, int parentExplorationId, Sink sink) {
            this.level = level;
            this.sink = sink;
            this.finisher = sink.writeLevelNode(id(), parentExplorationId, incomingState, level);
            this.startColumn = incomingState.column();
        }

        /**
         * @param incomingState the state when starting this exploration, whose indents might be different from those in
         *     this level's {@code incomingState}.
         */
        @Override
        public Exploration explore(
                String humanDescription, State incomingState, Function<ExplorationNode, State> explorationFunc) {
            ExplorationNodeImpl explorationNode =
                    new ExplorationNodeImpl(this, humanDescription, sink, startColumn, Optional.of(incomingState));
            State newState = explorationFunc.apply(explorationNode);
            explorationNode.recordNewState(Optional.of(newState));

            return new Exploration() {
                @Override
                public State markAccepted() {
                    finisher.finishNode(explorationNode.id());
                    return newState;
                }

                @Override
                public State state() {
                    return newState;
                }
            };
        }

        @Override
        public Optional<Exploration> maybeExplore(
                String humanDescription,
                State incomingState,
                Function<ExplorationNode, Optional<State>> explorationFunc) {
            ExplorationNodeImpl explorationNode =
                    new ExplorationNodeImpl(this, humanDescription, sink, startColumn, Optional.of(incomingState));
            Optional<State> maybeNewState = explorationFunc.apply(explorationNode);
            explorationNode.recordNewState(maybeNewState);

            if (!maybeNewState.isPresent()) {
                return Optional.empty();
            }

            State newState = maybeNewState.get();
            return Optional.of(new Exploration() {
                @Override
                public State markAccepted() {
                    finisher.finishNode(explorationNode.id());
                    return newState;
                }

                @Override
                public State state() {
                    return newState;
                }
            });
        }

        @Override
        public State finishLevel(State state) {
            // TODO save the state somehow
            // this final state will be different from the 'accepted' state
            return state;
        }
    }

    private static class ExplorationNodeImpl extends HasUniqueId implements ExplorationNode {
        private final Sink sink;
        private final FinishExplorationNode finishExplorationNode;
        private final Optional<Level> parentLevel;

        public ExplorationNodeImpl(
                LevelNodeImpl parent,
                String humanDescription,
                Sink sink,
                int startColumn,
                Optional<State> incomingState) {
            this.parentLevel = Optional.ofNullable(parent).map(p -> p.level);
            this.sink = sink;
            this.finishExplorationNode = sink.startExplorationNode(
                    id(),
                    parent != null ? OptionalInt.of(parent.id()) : OptionalInt.empty(),
                    humanDescription,
                    startColumn,
                    incomingState);
        }

        @Override
        public LevelNode newChildNode(Level level, State state) {
            return new LevelNodeImpl(level, state, id(), sink);
        }

        void recordNewState(Optional<State> maybeNewState) {
            maybeNewState.ifPresent(
                    newState -> parentLevel.ifPresent(parent -> finishExplorationNode.finishNode(parent, newState)));
        }
    }
}
