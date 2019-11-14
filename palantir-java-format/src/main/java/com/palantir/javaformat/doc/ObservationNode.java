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

public interface ObservationNode {

    static ObservationNode createRoot() {
        return null; // TODO(dfox)
    }

    State finishLevel(State updateAfterLevel);
    ObservationNode newChildNode(Level level, State state);


    /** At a single level, you can expore various options for how to break lines and then accept one. */
    ExplorationNode explore(String humanDescription, Function<ObservationNode, State> supplier);
    Optional<ExplorationNode> maybeExplore(String humanDescription, Function<ObservationNode, Optional<State>> supplier);

    interface ExplorationNode {
        State markAccepted();
        State state();
    }
}
