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

public interface Obs {

    interface Sink {
        void writeExplorationNode(int exporationId, OptionalInt parentLevelId, String humanDescription);

        void writeLevelNode(
                int levelId, int parentExplorationId, State incomingState, Level level, int acceptedExplorationId);

        String getOutput();
    }

    static ExplorationNode createRoot(Sink sink) {
        return ExplorationNodeImpl.createAndWrite(null, "(initial node)", sink);
    }

    /** At a single level, you can explore various options for how to break lines and then accept one. */
    interface LevelNode {

        Exploration explore(String humanDescription, Function<ExplorationNode, State> supplier);

        int id();

        Optional<Exploration> maybeExplore(
                String humanDescription, Function<ExplorationNode, Optional<State>> supplier);

        State finishLevel(State state);
    }

    class LevelNodeImpl implements LevelNode {
        private final Level level;
        private final State incomingState;
        private final int parentExplorationId;
        private final Sink sink;

        public LevelNodeImpl(Level level, State incomingState, int parentExplorationId, Sink sink) {
            this.level = level;
            this.incomingState = incomingState;
            this.parentExplorationId = parentExplorationId;
            this.sink = sink;
        }

        @Override
        public Exploration explore(String humanDescription, Function<ExplorationNode, State> explorationFunc) {

            ExplorationNode explorationNode = ExplorationNodeImpl.createAndWrite(this, humanDescription, sink);
            State newState = explorationFunc.apply(explorationNode);

            return new Exploration() {
                @Override
                public State markAccepted() {
                    sink.writeLevelNode(id(), parentExplorationId, incomingState, level, explorationNode.id());
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
                String humanDescription, Function<ExplorationNode, Optional<State>> explorationFunc) {

            ExplorationNode explorationNode = ExplorationNodeImpl.createAndWrite(this, humanDescription, sink);
            Optional<State> maybeNewState = explorationFunc.apply(explorationNode);

            if (!maybeNewState.isPresent()) {
                return Optional.empty();
            }

            State newState = maybeNewState.get();
            return Optional.of(new Exploration() {
                @Override
                public State markAccepted() {
                    // System.out.println("accepted exploration " + humanDescription);
                    return newState;
                }

                @Override
                public State state() {
                    return newState;
                }
            });
        }

        @Override
        public int id() {
            return level.uniqueId;
        }

        @Override
        public State finishLevel(State state) {
            // this final state will be different from the 'accepted' state
            return state;
        }
    }

    /** A handle that lets you accept exactly one 'exploration' of how to format a level. */
    interface Exploration {
        State markAccepted();

        State state();
    }

    /** Within an 'exploration', allows you to descend into child levels. */
    interface ExplorationNode {
        LevelNode newChildNode(Level level, State state);

        int id();
    }

    class ExplorationNodeImpl extends HasUniqueId implements ExplorationNode {
        private final Sink sink;

        public ExplorationNodeImpl(Sink sink) {
            this.sink = sink;
        }

        static ExplorationNode createAndWrite(LevelNode parent, String humanDescription, Sink sink) {
            ExplorationNodeImpl node = new ExplorationNodeImpl(sink);
            sink.writeExplorationNode(
                    node.id(), parent != null ? OptionalInt.of(parent.id()) : OptionalInt.empty(), humanDescription);
            return node;
        }

        @Override
        public LevelNode newChildNode(Level level, State state) {
            return new LevelNodeImpl(level, state, id(), sink);
        }

        @Override
        public int id() {
            return uniqueId;
        }
    }
}
