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
import com.palantir.javaformat.OpsBuilder.OpsOutput;
import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.BreakTag;
import com.palantir.javaformat.doc.Comment;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.FillMode;
import com.palantir.javaformat.doc.HtmlDocVisitor;
import com.palantir.javaformat.doc.Level;
import com.palantir.javaformat.doc.NonBreakingSpace;
import com.palantir.javaformat.doc.State;
import com.palantir.javaformat.doc.Token;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class DebugRenderer {

    public static void render(
            JavaInput javaInput, OpsOutput opsOutput, Level doc, State finalState, JavaOutput javaOutput) {

        StringBuilder sb = new StringBuilder();

        sb.append("<head>");
        sb.append("<style type=\"text/css\">"
                + "span.token:hover { outline: 1px solid black; } "
                + "span.token:hover span.token-body { text-decoration: underline; }"
                + "span.open-op, span.close-op, span.break-tag { position: relative;width: 3px;height: 1em;display: "
                + "inline-block; margin: 0 1px;}"
                + "span.open-op { background: green;}"
                + "span.close-op { background: red;}"
                + "span.break-tag.FillMode-UNIFIED { background: #777;}"
                + "span.break-tag.FillMode-INDEPENDENT { background: #333;}"
                + "span.break-tag.FillMode-FORCED { background: #000; width: 5px;}"
                + "span.break-tag.conditional { height: 0.4em; width: 0.4em; vertical-align:middle;}"
                + "</style>");
        sb.append("</head>");

        sb.append("<div style=\"white-space: pre; font-family: monospace; line-height: 1em;\">");

        sb.append("<h1>javaInput</h1>");
        sb.append("<code>");
        sb.append(javaInput.getText());
        sb.append("</code>");

        sb.append("<h1>List&lt;Op&gt;</h1>");
        sb.append("<p><i>Note: Comment and NonBreakingSpaces are not rendered here. Columns may be misaligned"
                + ".</i></p>");

        ImmutableList<Op> ops = opsOutput.ops();
        for (Op op : ops) {
            if (op instanceof Token) {
                sb.append(String.format("<span class=\"token\" style=\"%s\">", backgroundColor((Doc) op)));
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
                String conditional = breakTag.isPresent() ? "conditional" : "";
                FillMode fillMode = ((Break) op).fillMode();
                sb.append(String.format(
                        "<span class=\"break-tag %s FillMode-%s\" title=\"%s\"></span>",
                        conditional, fillMode, op.toString()));
            }
            if (op instanceof NonBreakingSpace) {}
            if (op instanceof Comment) {}

            if (op instanceof OpenOp) {
                sb.append("<span class=\"open-op\" title=\"" + op.toString() + "\"></span>");
            }
            if (op instanceof CloseOp) {
                sb.append("<span class=\"close-op\" title=\"" + op.toString() + "\"></span>");
            }
        }

        sb.append("<h1>Doc</h1>");
        sb.append("<code>");
        sb.append(new HtmlDocVisitor(finalState).visit(doc));
        sb.append("</code>");

        sb.append("<h1>javaOutput</h1>");
        sb.append("<code>");
        for (int i = 0; i < javaOutput.getLineCount(); ++i) {
            sb.append(javaOutput.getLine(i));
            sb.append("\n");
        }
        sb.append("</code>");

        sb.append("</div>");

        try {
            Files.write(
                    Paths.get(System.getProperty("user.home")).resolve("Desktop/output.html"),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String backgroundColor(Doc op) {
        long hue = Hashing.adler32().hashInt(op.uniqueId).padToLong() % 360;
        return String.format("background: hsl(%d, 60%%, 90%%)", hue);
    }
}
