package com.palantir.javaformat.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.javaformat.doc.JsonDocVisitor;
import com.palantir.javaformat.doc.Level;
import com.palantir.javaformat.doc.Obs.FinishExplorationNode;
import com.palantir.javaformat.doc.Obs.FinishLevelNode;
import com.palantir.javaformat.doc.Obs.Sink;
import com.palantir.javaformat.doc.State;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class JsonSink implements Sink {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

    private final Map<Integer, ArrayNode> childrenMap = new HashMap<>();
    private ObjectNode rootNode;

    @Override
    public FinishExplorationNode startExplorationNode(
            int explorationId, OptionalInt parentLevelId, String humanDescription, int startColumn) {
        ObjectNode json;
        if (parentLevelId.isPresent()) {
            json = childrenMap.get(parentLevelId.getAsInt()).addObject();
        } else {
            json = rootNode = OBJECT_MAPPER.createObjectNode();
        }
        json.put("type", "exploration");
        json.put("id", explorationId);
        parentLevelId.ifPresent(id -> json.put("parentId", id));
        json.put("humanDescription", humanDescription);
        // The column where we started off this exploration. Necessary to correctly indent the level.
        json.put("startColumn", startColumn);
        createChildrenNode(explorationId, json);
        return (parentLevel, newState) -> {
            json.set("outputLevel", new JsonDocVisitor(newState).visit(parentLevel));
        };
    }

    @Override
    public FinishLevelNode writeLevelNode(int levelNodeId, int parentExplorationId, State incomingState, Level level) {
        ObjectNode json = childrenMap.get(parentExplorationId).addObject();
        json.put("type", "level");
        json.put("id", levelNodeId);
        json.put("levelId", level.id());
        json.put("parentId", parentExplorationId);
        json.put("debugName", level.getDebugName().orElse(null));
        json.put("flat", level.getFlat());
        json.put("toString", level.toString());
        createChildrenNode(levelNodeId, json);
        return acceptedExplorationId -> json.put("acceptedExplorationId", acceptedExplorationId);
    }

    @Override
    public String getOutput() {
        try {
            return OBJECT_MAPPER.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void createChildrenNode(int id, ObjectNode json) {
        ArrayNode children = OBJECT_MAPPER.createArrayNode();
        json.set("children", children);
        childrenMap.put(id, children);
    }
}
