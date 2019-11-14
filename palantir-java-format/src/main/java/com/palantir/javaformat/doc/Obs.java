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
        return null;
    }

    /** At a single level, you can expore various options for how to break lines and then accept one. */
    interface LevelNode {

        Exploration explore(String humanDescription, Function<ExplorationNode, State> supplier);

        Optional<Exploration> maybeExplore(
                String humanDescription, Function<ExplorationNode, Optional<State>> supplier);

        default State finishLevel(State state) {
            return state;
        }
    }

    interface Exploration {
        State markAccepted();
        State state();
    }

    interface ExplorationNode {

        default LevelNode newChildNode(Level level, State state) {
            return new LevelNode() {
                @Override
                public Exploration explore(
                        String humanDescription, Function<ExplorationNode, State> exploreFunction) {

                    ExplorationNode todo = null;
                    State applied = exploreFunction.apply(todo);

                    return new Exploration() {
                        @Override
                        public State markAccepted() {
                            System.out.println("accepted " + humanDescription);
                            return applied;
                        }

                        @Override
                        public State state() {
                            return applied;
                        }
                    };
                }

                @Override
                public Optional<Exploration> maybeExplore(
                        String humanDescription, Function<ExplorationNode, Optional<State>> supplier) {
                    return Optional.empty();
                }
            };
        }
    }
}
