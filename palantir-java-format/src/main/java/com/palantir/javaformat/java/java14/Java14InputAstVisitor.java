/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat.java.java14;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.sun.source.tree.Tree.Kind.BLOCK;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.java.JavaInputAstVisitor;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.YieldTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.List;
import java.util.Optional;

/**
 * Extends {@link JavaInputAstVisitor} with support for AST nodes that were added or modified for Java 14.
 */
public class Java14InputAstVisitor extends JavaInputAstVisitor {

    public Java14InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
        super(builder, indentMultiplier);
    }

    @Override
    public Void visitBindingPattern(BindingPatternTree node, Void unused) {
        sync(node);
        scan(node.getType(), null);
        builder.breakOp(" ");
        visit(node.getBinding());
        return null;
    }

    @Override
    public Void visitYield(YieldTree node, Void aVoid) {
        sync(node);
        token("yield");
        builder.space();
        scan(node.getValue(), null);
        token(";");
        return null;
    }

    @Override
    public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
        sync(node);
        visitSwitch(node.getExpression(), node.getCases());
        return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void unused) {
        switch (tree.getKind()) {
            case ANNOTATION_TYPE:
                visitAnnotationType(tree);
                break;
            case CLASS:
            case INTERFACE:
                visitClassDeclaration(tree);
                break;
            case ENUM:
                visitEnumDeclaration(tree);
                break;
            case RECORD:
                visitRecordDeclaration(tree);
                break;
            default:
                throw new AssertionError(tree.getKind());
        }
        return null;
    }

    public void visitRecordDeclaration(ClassTree node) {
        sync(node);
        List<Op> breaks = visitModifiers(
                node.getModifiers(), Direction.VERTICAL, /* declarationAnnotationBreak= */ Optional.empty());
        Verify.verify(node.getExtendsClause() == null);
        boolean hasSuperInterfaceTypes = !node.getImplementsClause().isEmpty();
        builder.addAll(breaks);
        token("record");
        builder.space();
        visit(node.getSimpleName());
        if (!node.getTypeParameters().isEmpty()) {
            token("<");
        }
        builder.open(plusFour);
        {
            if (!node.getTypeParameters().isEmpty()) {
                typeParametersRest(node.getTypeParameters(), hasSuperInterfaceTypes ? plusFour : ZERO);
            }
            ImmutableList<JCTree.JCVariableDecl> parameters = compactRecordConstructor(node)
                    .map(m -> ImmutableList.copyOf(m.getParameters()))
                    .orElseGet(() -> recordVariables(node));
            token("(");
            if (!parameters.isEmpty()) {
                // Break before args.
                builder.breakToFill("");
            }
            // record headers can't declare receiver parameters
            visitFormals(/* receiver= */ Optional.empty(), parameters);
            token(")");
            if (hasSuperInterfaceTypes) {
                builder.breakToFill(" ");
                builder.open(node.getImplementsClause().size() > 1 ? plusFour : ZERO);
                token("implements");
                builder.space();
                boolean first = true;
                for (Tree superInterfaceType : node.getImplementsClause()) {
                    if (!first) {
                        token(",");
                        builder.breakOp(" ");
                    }
                    scan(superInterfaceType, null);
                    first = false;
                }
                builder.close();
            }
        }
        builder.close();
        if (node.getMembers() == null) {
            token(";");
        } else {
            List<Tree> members = node.getMembers().stream()
                    .filter(t -> (TreeInfo.flags((JCTree) t) & Flags.GENERATED_MEMBER) == 0)
                    .collect(toImmutableList());
            addBodyDeclarations(members, BracesOrNot.YES, FirstDeclarationsOrNot.YES);
        }
        dropEmptyDeclarations();
    }

    private static Optional<JCTree.JCMethodDecl> compactRecordConstructor(ClassTree node) {
        return node.getMembers().stream()
                .filter(JCTree.JCMethodDecl.class::isInstance)
                .map(JCTree.JCMethodDecl.class::cast)
                .filter(m -> (m.mods.flags & COMPACT_RECORD_CONSTRUCTOR) == COMPACT_RECORD_CONSTRUCTOR)
                .collect(toOptional());
    }

    private static ImmutableList<JCTree.JCVariableDecl> recordVariables(ClassTree node) {
        return node.getMembers().stream()
                .filter(JCTree.JCVariableDecl.class::isInstance)
                .map(JCTree.JCVariableDecl.class::cast)
                .filter(m -> (m.mods.flags & RECORD) == RECORD)
                .collect(toImmutableList());
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void unused) {
        sync(node);
        builder.open(plusFour);
        scan(node.getExpression(), null);
        builder.breakOp(" ");
        builder.open(ZERO);
        token("instanceof");
        builder.breakOp(" ");
        if (node.getPattern() != null) {
            scan(node.getPattern(), null);
        } else {
            scan(node.getType(), null);
        }
        builder.close();
        builder.close();
        return null;
    }

    @Override
    public Void visitCase(CaseTree node, Void unused) {
        sync(node);
        markForPartialFormat();
        builder.forcedBreak();
        if (node.getExpressions().isEmpty()) {
            token("default", plusTwo);
        } else {
            token("case", plusTwo);
            builder.space();
            builder.open(plusTwo);
            boolean first = true;
            for (ExpressionTree expression : node.getExpressions()) {
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
                        && node.getStatements().get(0).getKind() == BLOCK;
                builder.open(isBlock ? ZERO : plusTwo);
                if (isBlock) {
                    builder.space();
                }
                visitStatements(node.getStatements(), isBlock);
                builder.close();
                break;
            case RULE:
                builder.space();
                token("-");
                token(">");
                builder.space();
                if (node.getBody().getKind() == BLOCK) {
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
                throw new AssertionError(node.getCaseKind());
        }
        return null;
    }

    /**
     * TODO(fwindheuser): Collapse with
     * {@link JavaInputAstVisitor#visitLambdaExpression(LambdaExpressionTree, Void)}} after dropping Java 11
     * compatibility.
     */
    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        sync(node);
        // Also format switch expressions as statement body instead of inlining them
        boolean statementBody = node.getBodyKind() == LambdaExpressionTree.BodyKind.STATEMENT
                || node.getBody().getKind() == Kind.SWITCH_EXPRESSION;
        visitLambdaExpression(node, statementBody);
        return null;
    }
}
