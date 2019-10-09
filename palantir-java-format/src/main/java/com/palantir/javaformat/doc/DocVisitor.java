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

public interface DocVisitor<T> {
  default T visit(Doc doc) {
    if (doc instanceof Level) {
      return visitLevel((Level) doc);
    } else if (doc instanceof Break) {
      return visitBreak((Break) doc);
    } else if (doc instanceof Token) {
      return visitToken((Token) doc);
    } else if (doc instanceof Tok) {
      return visitTok((Tok) doc);
    } else if (doc instanceof Space) {
      return visitSpace((Space) doc);
    }
    throw new RuntimeException();
  }

  T visitSpace(Space doc);

  T visitTok(Tok doc);

  T visitToken(Token doc);

  T visitBreak(Break doc);

  T visitLevel(Level doc);
}
