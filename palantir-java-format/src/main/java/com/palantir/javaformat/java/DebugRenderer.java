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

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.palantir.javaformat.CloseOp;
import com.palantir.javaformat.Input;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpenOp;
import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.BreakTag;
import com.palantir.javaformat.doc.Comment;
import com.palantir.javaformat.doc.NonBreakingSpace;
import com.palantir.javaformat.doc.Token;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class DebugRenderer {

    public static void render(JavaInput javaInput, OpsBuilder.OpsOutput opsOutput) {

        StringBuilder sb = new StringBuilder();

        sb.append("<head>");
        sb.append("<style type=\"text/css\">"
                + "span.token:hover { outline: 1px solid black } "
                + "span.token:hover span.token-body { text-decoration: underline }"
                + "</style>");
        sb.append("</head>");

        sb.append("<div style=\"white-space: pre; font-family: monospace\">");

        sb.append("<h1>javaInput</h1>");
        sb.append("<code>");
        sb.append(javaInput.getText());
        sb.append("</code>");

        sb.append("<h1>List&lt;Op&gt;</h1>");

        ImmutableList<Op> ops = opsOutput.ops();
        for (Op op : ops) {
            if (op instanceof Token) {
                long hue = Hashing.adler32().hashInt(((Token) op).uniqueId).padToLong() % 360;

                sb.append(String.format("<span class=\"token\" style=\"background: hsl(%d, 60%%, 90%%)\">", hue));
                Input.Token foo = ((Token) op).getToken();
                foo.getToksBefore().forEach(before -> {
                    sb.append(before.getText());
                });

                sb.append("<span class=\"token-body\">");
                sb.append(foo.getTok().getText());
                sb.append("</span>");

                foo.getToksAfter().forEach(before -> {
                    sb.append(before.getText());
                });
                sb.append("</span>");
            }
            if (op instanceof Break) {
                Optional<BreakTag> breakTag = ((Break) op).optTag();
                // sb.append("break?" + breakTag.map(t -> Integer.toString(t.uniqueId)).orElse(""));
            }
            if (op instanceof NonBreakingSpace) {
                // sb.append("#");
            }
            if (op instanceof Comment) {}
            if (op instanceof OpenOp) {
                OpenOp openOp = (OpenOp) op;
            }
            if (op instanceof CloseOp) {
                CloseOp closeOp = (CloseOp) op;
            }
        }

        sb.append("</div>");

        try {
            Files.write(Paths.get("/Users/dfox/Desktop/output.html"), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
