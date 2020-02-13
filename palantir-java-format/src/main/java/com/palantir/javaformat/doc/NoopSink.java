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

import com.palantir.javaformat.doc.Obs.FinishExplorationNode;
import com.palantir.javaformat.doc.Obs.FinishLevelNode;
import com.palantir.javaformat.doc.Obs.Sink;
import java.util.Optional;
import java.util.OptionalInt;

public final class NoopSink implements Sink {
    @Override
    public FinishExplorationNode startExplorationNode(
            int exporationId,
            OptionalInt parentLevelId,
            String humanDescription,
            int startColumn,
            Optional<State> incomingState) {
        return (parentLevel, newState) -> {};
    }

    @Override
    public FinishLevelNode writeLevelNode(int levelNodeId, int parentExplorationId, State incomingState, Level level) {
        return acceptedExplorationId -> {};
    }

    @Override
    public String getOutput() {
        throw new RuntimeException("Not supposed to get the output");
    }
}
