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
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.palantir.javaformat.CloseOp;
import com.palantir.javaformat.Input;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpenOp;
import com.palantir.javaformat.OpsBuilder.OpsOutput;
import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.Comment;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.Level;
import com.palantir.javaformat.doc.NonBreakingSpace;
import com.palantir.javaformat.doc.State;
import com.palantir.javaformat.doc.Token;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class DebugRenderer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static void render(JavaInput javaInput, OpsOutput opsOutput, Level _doc, State _finalState, JavaOutput javaOutput) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta charset=\"utf-8\">\n");
        // TODO(dfox): import the script built by App.tsx?
        sb.append("<script type=\"text/javascript\">\n");

        sb.append(String.format(
                "window.palantirJavaFormat = {\njavaInput: %s,\nops: %s,\ndoc: {},\njavaOutput: %s\n};\n",
                jsonEscapedString(javaInput.getText()),
                opsJson(opsOutput),
                jsonEscapedString(outputAsString(javaOutput))));

        sb.append("</script>\n");
        sb.append("</head>\n");
        sb.append("<body><div id=\"root\"></div></body>\n");
        sb.append("</html>\n");

        try {
            Files.write(
                    Paths.get(System.getProperty("user.home")).resolve("Desktop/output.html"),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String outputAsString(JavaOutput javaOutput) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < javaOutput.getLineCount(); ++i) {
            output.append(javaOutput.getLine(i));
            output.append("\n");
        }
        return output.toString();
    }

    private static String opsJson(OpsOutput opsOutput) {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();

        ImmutableList<Op> ops = opsOutput.ops();
        for (Op op : ops) {
            if (op instanceof Token) {
                Input.Token token = ((Token) op).getToken();

                ObjectNode json = arrayNode.addObject();
                json.put("type", "token");
                json.put(
                        "beforeText",
                        token.getToksBefore().stream().map(Input.Tok::getText).collect(Collectors.joining()));
                json.put("text", token.getTok().getText());
                json.put(
                        "afterText",
                        token.getToksAfter().stream().map(Input.Tok::getText).collect(Collectors.joining()));
            }
            if (op instanceof Break) {
                Break breakOp = (Break) op;

                ObjectNode json = arrayNode.addObject();
                json.put("type", "break");
                json.put("fillMode", breakOp.fillMode().toString());
                json.put("toString", op.toString());
                breakOp.optTag().ifPresent(tag -> json.put("breakTag", tag.uniqueId));
            }
            if (op instanceof NonBreakingSpace) {
                ObjectNode json = arrayNode.addObject();
                json.put("type", "nonBreakingSpace");
                json.put("toString", op.toString());
            }
            if (op instanceof Comment) {
                ObjectNode json = arrayNode.addObject();
                json.put("type", "comment");
                json.put("toString", op.toString());
            }
            if (op instanceof OpenOp) {
                ObjectNode json = arrayNode.addObject();
                json.put("type", "openOp");
                json.put("toString", op.toString());
            }
            if (op instanceof CloseOp) {
                ObjectNode json = arrayNode.addObject();
                json.put("type", "closeOp");
                json.put("toString", op.toString());
            }
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String jsonEscapedString(String javaInput) {
        try {
            return OBJECT_MAPPER.writeValueAsString(javaInput);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String backgroundColor(Doc op) {
        long hue = Hashing.adler32().hashInt(op.uniqueId).padToLong() % 360;
        return String.format("background: hsl(%d, 60%%, 90%%)", hue);
    }
}
