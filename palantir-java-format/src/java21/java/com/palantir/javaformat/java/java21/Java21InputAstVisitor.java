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

import com.google.common.collect.Iterables;
import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.java.JavaInputAstVisitor;
import com.palantir.javaformat.java.java14.Java14InputAstVisitor;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.DeconstructionPatternTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.PatternTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Extends {@link Java14InputAstVisitor} with support for AST nodes that were added or modified for Java 14.
 */
public class Java21InputAstVisitor extends Java14InputAstVisitor {
    private static final Method CASE_TREE_GET_LABELS2 = maybeGetMethodPrivate(CaseTree.class, "getLabels");

    private static Method maybeGetMethodPrivate(Class<?> c, String name) {
        try {
            return c.getMethod(name);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object invokePrivate(Method m, Object target) {
        try {
            return m.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Java21InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
        super(builder, indentMultiplier);
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree node, Void unused) {
        token("(");
        Void v = scan(node.getExpression(), unused);
        token(")");
        return v;
    }

    @Override
    public Void visitDeconstructionPattern(DeconstructionPatternTree node, Void unused) {

        Void r = scan(node.getDeconstructor(), unused);
        token("(");
        boolean firstInRow = true;
        for (PatternTree item : node.getNestedPatterns()) {
            if (!firstInRow) {
                token(",");
                builder.breakToFill(" ");
            }
            scan(item, null);
            firstInRow = false;
        }
        token(")");
        return r;
    }

    @Override
    public Void visitConstantCaseLabel(ConstantCaseLabelTree node, Void unused) {
        return scan(node.getConstantExpression(), unused);
    }

    @Override
    public Void visitCase(CaseTree node, Void unused) {
        sync(node);
        markForPartialFormat();
        builder.forcedBreak();
        List<? extends Tree> labels;
        boolean isDefault;
        if (CASE_TREE_GET_LABELS2 != null) {
            labels = (List<? extends Tree>) invokePrivate(CASE_TREE_GET_LABELS2, node);
            isDefault = labels.size() == 1
                    && Iterables.getOnlyElement(labels).getKind().name().equals("DEFAULT_CASE_LABEL");
        } else {
            labels = node.getExpressions();
            isDefault = labels.isEmpty();
        }
        if (isDefault) {
            token("default", plusTwo);
        } else {
            token("case", plusTwo);
            builder.space();
            builder.open(labels.size() > 1 ? plusFour : JavaInputAstVisitor.ZERO);
            boolean first = true;
            for (Tree expression : labels) {
                if (!first) {
                    token(",");
                    builder.breakOp(" ");
                }
                scan(expression, null);
                first = false;
            }
            builder.close();
        }
        switch (node.getCaseKind()) {
            case STATEMENT:
                token(":");
                boolean isBlock = node.getStatements().size() == 1
                        && node.getStatements().get(0).getKind() == Kind.BLOCK;
                builder.open(isBlock ? JavaInputAstVisitor.ZERO : plusTwo);
                if (isBlock) {
                    builder.space();
                }
                visitStatements(node.getStatements(), isBlock);
                builder.close();
                break;
            case RULE:
                if (node.getGuard() != null) {
                    builder.space();
                    token("when");
                    builder.space();
                    scan(node.getGuard(), null);
                }

                builder.space();
                token("-");
                token(">");

                builder.space();
                if (node.getBody().getKind() == Kind.BLOCK) {
                    // Explicit call with {@link CollapseEmptyOrNot.YES} to handle empty case blocks.
                    visitBlock(
                            (BlockTree) node.getBody(),
                            CollapseEmptyOrNot.YES,
                            AllowLeadingBlankLine.NO,
                            AllowTrailingBlankLine.NO);
                } else {
                    scan(node.getBody(), null);
                }
                builder.guessToken(";");
                break;
            default:
                throw new IllegalArgumentException(node.getCaseKind().name());
        }
        return null;
    }

    public Void visitPatternCaseLabel(PatternCaseLabelTree node, Void p) {
        return scan(node.getPattern(), p);
    }
}
