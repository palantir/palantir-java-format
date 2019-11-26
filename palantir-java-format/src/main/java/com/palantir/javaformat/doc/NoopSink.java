package com.palantir.javaformat.doc;

import com.palantir.javaformat.doc.Obs.FinishExplorationNode;
import com.palantir.javaformat.doc.Obs.FinishLevelNode;
import com.palantir.javaformat.doc.Obs.Sink;
import java.util.OptionalInt;

public final class NoopSink implements Sink {
    @Override
    public FinishExplorationNode startExplorationNode(
            int exporationId, OptionalInt parentLevelId, String humanDescription) {
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
