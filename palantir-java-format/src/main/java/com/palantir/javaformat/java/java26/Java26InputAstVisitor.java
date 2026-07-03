/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat.java.java26;

import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.java.java25.Java25InputAstVisitor;

/**
 * Extends {@link Java25InputAstVisitor} with support for AST nodes that were added or modified in
 * Java 26.
 */
public class Java26InputAstVisitor extends Java25InputAstVisitor {
    public Java26InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
        super(builder, indentMultiplier);
    }
}
