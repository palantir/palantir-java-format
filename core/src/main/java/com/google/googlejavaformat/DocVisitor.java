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

package com.google.googlejavaformat;

import com.google.googlejavaformat.Doc.Break;
import com.google.googlejavaformat.Doc.Level;
import com.google.googlejavaformat.Doc.Space;
import com.google.googlejavaformat.Doc.Tok;
import com.google.googlejavaformat.Doc.Token;

public interface DocVisitor<T> {
  default T visit(Doc doc) {
    if (doc instanceof Doc.Level) {
      return visitLevel((Level) doc);
    } else if (doc instanceof Doc.Break) {
      return visitBreak((Break) doc);
    } else if (doc instanceof Doc.Token) {
      return visitToken((Token) doc);
    } else if (doc instanceof Doc.Tok) {
      return visitTok((Tok) doc);
    } else if (doc instanceof Doc.Space) {
      return visitSpace((Space) doc);
    }
    throw new RuntimeException();
  }

  T visitSpace(Doc.Space doc);

  T visitTok(Doc.Tok doc);

  T visitToken(Doc.Token doc);

  T visitBreak(Doc.Break doc);

  T visitLevel(Doc.Level doc);
}
