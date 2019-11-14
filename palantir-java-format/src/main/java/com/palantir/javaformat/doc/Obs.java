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
import java.util.function.Function;

public interface Obs {

    static ExplorationNode createRoot() {
        return new ExplorationNodeImpl();
    }

    /** At a single level, you can explore various options for how to break lines and then accept one. */
    interface LevelNode {

        Exploration explore(String humanDescription, Function<ExplorationNode, State> supplier);

        Optional<Exploration> maybeExplore(
                String humanDescription, Function<ExplorationNode, Optional<State>> supplier);

        State finishLevel(State state);
    }

    class LevelNodeImpl implements LevelNode {

        @Override
        public Exploration explore(String humanDescription, Function<ExplorationNode, State> explorationFunc) {

            ExplorationNode explorationNode = new ExplorationNodeImpl();
            State newState = explorationFunc.apply(explorationNode);

            return new Exploration() {
                @Override
                public State markAccepted() {
                    // System.out.println("accepted exploration " + humanDescription);
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

            ExplorationNode explorationNode = new ExplorationNodeImpl();
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
    }

    class ExplorationNodeImpl implements ExplorationNode {
        @Override
        public LevelNode newChildNode(Level level, State state) {
            return new LevelNodeImpl();
        }
    }
}
