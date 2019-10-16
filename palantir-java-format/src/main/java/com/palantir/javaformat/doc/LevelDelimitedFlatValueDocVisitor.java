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
import com.palantir.javaformat.BreakBehaviour;
import com.palantir.javaformat.Breakability;
import com.palantir.javaformat.Indent;

public final class LevelDelimitedFlatValueDocVisitor implements DocVisitor<String> {
    int indent = 0;

    @Override
    public String visitSpace(Space doc) {
        return doc.getFlat();
    }

    @Override
    public String visitTok(Tok doc) {
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
        if (!doc.getPlusIndent().equals(Indent.Const.ZERO)) {
            sb.append(" +").append(doc.evalPlusIndent());
        }
        doc.getOptTag().ifPresent(optTag -> sb.append(" (").append(optTag).append(")"));
        return sb.toString();
    }

    @Override
    public String visitLevel(Level level) {
        if (level.getFlat().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("❰");
        if (!level.getPlusIndent().equals(Indent.Const.ZERO)) {
            builder.append(" +" + level.getPlusIndent().eval());
            if (level.getPlusIndent() instanceof Indent.If) {
                builder.append(" (").append(level.getPlusIndent()).append(")");
            }
        }
        if (level.getBreakBehaviour() != BreakBehaviour.BREAK_THIS_LEVEL) {
            builder.append(" ");
            builder.append(level.getBreakBehaviour());
        }
        if (level.getBreakabilityIfLastLevel() != Breakability.NO_PREFERENCE) {
            builder.append(" ifLastLevel=");
            builder.append(level.getBreakabilityIfLastLevel());
        }

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
        builder.append("❱");
        return builder.toString();
    }

    private void generateIndent(StringBuilder builder) {
        builder.append(Strings.repeat(" ", indent));
    }
}
