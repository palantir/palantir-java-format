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

package com.palantir.javaformat;

import com.google.common.base.Strings;

public final class LevelDelimitedFlatValueDocVisitor implements DocVisitor<String> {
  int indent = 0;

  @Override
  public String visitSpace(Doc.Space doc) {
    return doc.getFlat();
  }

  @Override
  public String visitTok(Doc.Tok doc) {
    return doc.getFlat();
  }

  @Override
  public String visitToken(Doc.Token doc) {
    return doc.getFlat();
  }

  @Override
  public String visitBreak(Doc.Break doc) {
    StringBuilder sb =
        new StringBuilder()
            .append("⏎")
            .append(doc.getFlat().isEmpty() ? "" : "(" + doc.getFlat() + ")");
    if (!doc.plusIndent.equals(Indent.Const.ZERO)) {
      sb.append(" +" + doc.plusIndent.eval());
    }
    return sb.toString();
  }

  @Override
  public String visitLevel(Doc.Level level) {
    if (level.getFlat().isEmpty()) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    builder.append("《");
    if (!level.getPlusIndent().equals(Indent.Const.ZERO)) {
      builder.append(" +" + level.getPlusIndent().eval());
    }

    indent += 2;
    boolean breakNext = true;

    for (Doc doc : level.getDocs()) {
      if (breakNext) {
        builder.append("\n");
        generateIndent(builder);
      }
      breakNext = doc instanceof Doc.Level || doc instanceof Doc.Break;
      builder.append(visit(doc));
    }
    indent -= 2;
    builder.append("\n");
    generateIndent(builder);
    builder.append("》");
    return builder.toString();
  }

  private void generateIndent(StringBuilder builder) {
    builder.append(Strings.repeat(" ", indent));
  }
}
