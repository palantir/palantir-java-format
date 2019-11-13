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

import com.google.common.base.Strings;
import com.palantir.javaformat.BreakBehaviours;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.LastLevelBreakability;
import com.palantir.javaformat.java.DebugRenderer;

public final class HtmlDocVisitor implements DocVisitor<String> {
    private final State state;
    int indent = 0;

    public HtmlDocVisitor(State state) {
        this.state = state;
    }

    @Override
    public String visitSpace(NonBreakingSpace doc) {
        return doc.getFlat();
    }

    @Override
    public String visitComment(Comment doc) {
        return doc.getFlat();
    }

    @Override
    public String visitToken(Token doc) {
        return doc.getFlat();
    }

    @Override
    public String visitBreak(Break doc) {
        StringBuilder sb =
                new StringBuilder().append("⏎").append(doc.getFlat().isEmpty() ? "" : "(" + doc.getFlat() + ")");
        if (!doc.plusIndent().equals(Indent.Const.ZERO)) {
            sb.append(" +" + doc.evalPlusIndent(state));
        }
        return sb.toString();
    }

    @Override
    public String visitLevel(Level level) {
        if (level.getFlat().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String titleStr = computeTitle(level, state);
        builder.append(String.format(
                "<span class=\"level\" style=\"%s\" title=\"%s\">",
                DebugRenderer.backgroundColor(level), titleStr));

        // References break tag

        indent += 2;
        boolean breakNext = true;

        for (Doc doc : level.getDocs()) {
            if (breakNext) {
                builder.append("\n");
                generateIndent(builder);
            }
            breakNext = doc instanceof Level || doc instanceof Break;
            builder.append(visit(doc));
        }
        indent -= 2;
        builder.append("\n");
        generateIndent(builder);
        builder.append("</span>");
        return builder.toString();
    }

    private static String computeTitle(Level level, State state) {
        StringBuilder title = new StringBuilder();
        if (!level.getPlusIndent().equals(Indent.Const.ZERO)) {
            title.append(" +" + level.getPlusIndent().eval(state));
        }
        BreakBehaviours.caseOf(level.getBreakBehaviour()).breakThisLevel_(null).otherwise(() -> {
            title.append(" ");
            title.append(level.getBreakBehaviour());
            return null;
        });
        if (level.getBreakabilityIfLastLevel() != LastLevelBreakability.ABORT) {
            title.append(" ifLastLevel=");
            title.append(level.getBreakabilityIfLastLevel());
        }

        return title.toString();
    }

    private void generateIndent(StringBuilder builder) {
        builder.append(Strings.repeat(" ", indent));
    }
}
