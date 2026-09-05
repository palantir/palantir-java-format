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

package com.palantir.javaformat.java.java25;

import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.java.java21.Java21InputAstVisitor;
import com.sun.source.tree.ImportTree;
import java.lang.reflect.Method;

/**
 * Extends {@link Java21InputAstVisitor} with support for AST nodes that were added or modified in
 * Java 25.
 */
public class Java25InputAstVisitor extends Java21InputAstVisitor {
    // ImportTree#isModule() was added in JDK 23 (JEP 511, module import declarations, GA in Java
    // 25). This module compiles at an older --release, so it can't be referenced directly.
    private static final Method IMPORT_TREE_IS_MODULE = maybeGetMethod(ImportTree.class, "isModule");

    public Java25InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
        super(builder, indentMultiplier);
    }

    @Override
    public Void visitImport(ImportTree node, Void unused) {
        sync(node);
        token("import");
        builder.space();
        if (IMPORT_TREE_IS_MODULE != null && Boolean.TRUE.equals(invoke(IMPORT_TREE_IS_MODULE, node))) {
            token("module");
            builder.space();
        } else if (node.isStatic()) {
            token("static");
            builder.space();
        }
        visitName(node.getQualifiedIdentifier());
        token(";");
        // TODO(cushon): remove this if https://bugs.openjdk.java.net/browse/JDK-8027682 is fixed
        dropEmptyDeclarations();
        return null;
    }
}
