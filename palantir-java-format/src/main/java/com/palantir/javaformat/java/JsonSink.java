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
import java.util.Optional;
import java.util.OptionalInt;

public final class JsonSink implements Sink {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

    private final Map<Integer, ArrayNode> childrenMap = new HashMap<>();
    private ObjectNode rootNode;

    @Override
    public FinishExplorationNode startExplorationNode(
            int explorationId,
            OptionalInt parentLevelId,
            String humanDescription,
            int startColumn,
            Optional<State> incomingState) {
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
        incomingState.ifPresent(state -> json.set("incomingState", OBJECT_MAPPER.valueToTree(state)));
        createChildrenNode(explorationId, json);
        return (parentLevel, newState) -> {
            ObjectNode resultNode = OBJECT_MAPPER.createObjectNode();
            json.set("result", resultNode);
            resultNode.set("outputLevel", new JsonDocVisitor(newState).visit(parentLevel));
            resultNode.set("finalState", OBJECT_MAPPER.valueToTree(newState));
        };
    }

    @Override
    public FinishLevelNode writeLevelNode(int levelNodeId, int parentExplorationId, State incomingState, Level level) {
        ObjectNode json = childrenMap.get(parentExplorationId).addObject();
        json.put("type", "level");
        json.put("id", levelNodeId);
        json.put("levelId", level.id());
        json.put("parentId", parentExplorationId);
        json.put("flat", level.getFlat());
        json.put("toString", level.toString());
        json.set("incomingState", OBJECT_MAPPER.valueToTree(incomingState));
        json.set("openOp", OBJECT_MAPPER.valueToTree(level.getOpenOp()));
        json.put("evaluatedIndent", level.getPlusIndent().eval(incomingState));
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
