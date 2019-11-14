package com.palantir.javaformat.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.palantir.javaformat.doc.Level;
import com.palantir.javaformat.doc.Obs.Sink;
import com.palantir.javaformat.doc.State;
import java.util.OptionalInt;

public final class JsonSink implements Sink {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ArrayNode nodes;

    public JsonSink() {
        nodes = OBJECT_MAPPER.createArrayNode();
    }

    @Override
    public void writeExplorationNode(int exporationId, OptionalInt parentLevelId, String humanDescription) {
        ObjectNode json = nodes.addObject();
        json.put("type", "exploration");
        json.put("id", exporationId);
        parentLevelId.ifPresent(id -> json.put("parentId", id));
        json.put("humanDescription", humanDescription);
    }

    @Override
    public void writeLevelNode(
            int levelId, int parentExplorationId, State incomingState, Level level, int acceptedExplorationId) {
        ObjectNode json = nodes.addObject();
        json.put("type", "level");
        json.put("id", levelId);
        json.put("parentId", parentExplorationId);
        json.put("debugName", level.getDebugName().orElse(null));
        json.put("flat", level.getFlat());
        json.put("toString", level.toString());
        json.put("acceptedExplorationId", acceptedExplorationId);
    }

    @Override
    public String getOutput() {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
