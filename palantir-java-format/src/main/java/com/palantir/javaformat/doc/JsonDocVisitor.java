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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class JsonDocVisitor implements DocVisitor<JsonNode> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final State state;

    public JsonDocVisitor(State state) {
        this.state = state;
    }

    @Override
    public JsonNode visitSpace(NonBreakingSpace doc) {
        return MAPPER.createObjectNode().put("type", "space").put("id", doc.id());
    }

    @Override
    public JsonNode visitComment(Comment doc) {
        return MAPPER.createObjectNode()
                .put("type", "comment")
                .put("flat", doc.getFlat())
                .put("text", state.getTokText(doc))
                .put("id", doc.id());
    }

    @Override
    public JsonNode visitToken(Token doc) {
        return MAPPER.createObjectNode()
                .put("type", "token")
                .put("flat", doc.getFlat())
                .put("id", doc.id());
    }

    @Override
    public JsonNode visitBreak(Break doc) {
        return MAPPER.createObjectNode()
                .put("type", "break")
                .put("flat", doc.getFlat())
                .set("breakState", MAPPER.valueToTree(state.getBreakState(doc)))
                .set("plusIndent", MAPPER.valueToTree(doc.plusIndent()))
                .set("optTag", MAPPER.valueToTree(doc.optTag()))
                .put("evalPlusIndent", doc.evalPlusIndent(state))
                .put("id", doc.id());
    }

    @Override
    public JsonNode visitLevel(Level level) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "level");
        node.put("flat", level.getFlat());
        node.put("id", level.id());
        node.set("openOp", MAPPER.valueToTree(level.getOpenOp()));
        node.put("isOneLine", state.isOneLine(level));
        node.put("evalPlusIndent", level.getPlusIndent().eval(state));
        node.set("docs", MAPPER.valueToTree(level.getDocs().stream().map(this::visit)));
        return node;
    }
}
