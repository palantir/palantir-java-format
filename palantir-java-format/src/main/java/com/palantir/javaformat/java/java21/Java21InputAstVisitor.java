/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat.java.java21;

import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.java.java14.Java14InputAstVisitor;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
/**
 * Extends {@link Java14InputAstVisitor} with support for AST nodes that were added or modified in
 * Java 21.
 */
public class Java21InputAstVisitor extends Java14InputAstVisitor {
    public Java21InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
        super(builder, indentMultiplier);
    }

    @Override
    protected ExpressionTree getGuard(final CaseTree node) {
        return node.getGuard();
    }
}