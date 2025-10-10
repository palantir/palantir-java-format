/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.palantir.javaformat.java;

import static com.google.common.collect.Iterables.getLast;
import static java.util.stream.Collectors.joining;

import com.google.common.base.CharMatcher;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;
import com.palantir.javaformat.Newlines;
import com.palantir.javaformat.Utils;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.Position;
import com.sun.tools.javac.util.Position.LineMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Wraps string literals that exceed the column limit. */
public final class StringWrapper {

    public static final String TEXT_BLOCK_DELIMITER = "\"\"\"";

    /** Reflows string literals in the given Java source code that extend past the given column limit. */
    static String wrap(final int columnLimit, String input, Formatter formatter) throws FormatterException {
        if (!needWrapping(columnLimit, input)) {
            // fast path
            return input;
        }

        TreeRangeMap<Integer, String> replacements = getReflowReplacements(columnLimit, input);
        String firstPass =
                formatter.formatSource(input, replacements.asMapOfRanges().keySet());

        if (!firstPass.equals(input)) {
            // If formatting the replacement ranges resulted in a change, recalculate the replacements on
            // the updated input.
            input = firstPass;
            replacements = getReflowReplacements(columnLimit, input);
        }

        String result = applyReplacements(input, replacements);

        // Format again, because broken strings might now fit on the first line in case of assignments
        String secondPass = formatter.formatSource(result, rangesAfterAppliedReplacements(replacements));

        if (!secondPass.equals(result)) {
            replacements = getReflowReplacements(columnLimit, secondPass);
            result = applyReplacements(secondPass, replacements);
        }

        {
            // We really don't want bugs in this pass to change the behaviour of programs we're
            // formatting, so check that the pretty-printed AST is the same before and after reformatting.
            String expected = parse(input, /* allowStringFolding= */ true).toString();
            String actual = parse(result, /* allowStringFolding= */ true).toString();
            if (!expected.equals(actual)) {
                throw new FormatterException(String.format(
                        "Something has gone terribly wrong. Please file a bug: "
                                + "https://github.com/palantir/palantir-java-format/issues/new"
                                + "\n\n=== Actual: ===\n%s\n=== Expected: ===\n%s\n",
                        actual, expected));
            }
        }

        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static ImmutableSet<Range<Integer>> rangesAfterAppliedReplacements(
            TreeRangeMap<Integer, String> replacements) {
        ImmutableSet.Builder<Range<Integer>> outputRanges = ImmutableSet.builder();
        int offset = 0;
        for (Map.Entry<Range<Integer>, String> entry :
                replacements.asMapOfRanges().entrySet()) {
            Range<Integer> range = entry.getKey();
            String replacement = entry.getValue();

            int lower = offset + range.lowerEndpoint();
            int upper = lower + replacement.length();
            outputRanges.add(Range.closedOpen(lower, upper));

            int originalLength = range.upperEndpoint() - range.lowerEndpoint();
            int newLength = upper - lower;
            offset += newLength - originalLength;
        }
        return outputRanges.build();
    }

    @SuppressWarnings("for-rollout:NullAway")
    public static TreeRangeMap<Integer, String> getReflowReplacements(int columnLimit, final String input)
            throws FormatterException {
        return new Reflower(columnLimit, input).getReflowReplacements();
    }

    private static class Reflower {

        private final String input;
        private final int columnLimit;
        private final String separator;
        private final JCTree.JCCompilationUnit unit;
        private final Position.LineMap lineMap;

        Reflower(int columnLimit, String input) throws FormatterException {
            this.columnLimit = columnLimit;
            this.input = input;
            this.separator = Newlines.guessLineSeparator(input);
            this.unit = parse(input, /* allowStringFolding= */ false);
            this.lineMap = unit.getLineMap();
        }

        protected TreeRangeMap<Integer, String> getReflowReplacements() {
            // Paths to string literals that extend past the column limit.
            List<TreePath> longStringLiterals = new ArrayList<>();
            // Paths to text blocks to be re-indented.
            List<TreePath> textBlocks = new ArrayList<>();
            new LongStringsAndTextBlockScanner(longStringLiterals, textBlocks).scan(new TreePath(unit), null);
            TreeRangeMap<Integer, String> replacements = TreeRangeMap.create();
            indentTextBlocks(replacements, textBlocks);
            wrapLongStrings(replacements, longStringLiterals);
            return replacements;
        }

        private class LongStringsAndTextBlockScanner extends TreePathScanner<Void, Void> {

            private final List<TreePath> longStringLiterals;
            private final List<TreePath> textBlocks;

            LongStringsAndTextBlockScanner(List<TreePath> longStringLiterals, List<TreePath> textBlocks) {
                this.longStringLiterals = longStringLiterals;
                this.textBlocks = textBlocks;
            }

            @Override
            public Void visitLiteral(LiteralTree literalTree, Void aVoid) {
                if (literalTree.getKind() != Kind.STRING_LITERAL) {
                    return null;
                }
                int pos = getStartPosition(literalTree);
                if (input.substring(pos, Math.min(input.length(), pos + 3)).equals(TEXT_BLOCK_DELIMITER)) {
                    textBlocks.add(getCurrentPath());
                    return null;
                }
                Tree parent = getCurrentPath().getParentPath().getLeaf();
                if (parent instanceof MemberSelectTree
                        && ((MemberSelectTree) parent).getExpression().equals(literalTree)) {
                    return null;
                }
                int endPosition = getEndPosition(unit, literalTree);
                int lineEnd = endPosition;
                while (Newlines.hasNewlineAt(input, lineEnd) == -1) {
                    lineEnd++;
                }
                if (lineMap.getColumnNumber(lineEnd) - 1 <= columnLimit) {
                    return null;
                }
                longStringLiterals.add(getCurrentPath());
                return null;
            }
        }

        private void indentTextBlocks(TreeRangeMap<Integer, String> replacements, List<TreePath> textBlocks) {
            // pjf specific - compute textBlock's parents & store info about the indentation\
            Map<TreePath, String> textBlockToIndent = computeCustomTextBlocksIndent(textBlocks);

            for (TreePath treePath : textBlocks) {
                Tree tree = treePath.getLeaf();
                int startPosition = getStartPosition(tree);
                int endPosition = getEndPosition(unit, tree);
                String text = input.substring(startPosition, endPosition);
                int lineStartPosition = lineMap.getStartPosition(lineMap.getLineNumber(startPosition));
                int startColumn =
                        CharMatcher.whitespace().negate().indexIn(input.substring(lineStartPosition, endPosition));

                // Find the source code of the text block with incidental whitespace removed.
                // The first line of the text block is always """, and it does not affect incidental
                // whitespace.
                ImmutableList<String> initialLines = text.lines().collect(ImmutableList.toImmutableList());
                String stripped = initialLines.stream()
                        .skip(1)
                        .collect(joining(separator))
                        .stripIndent();
                ImmutableList<String> lines = stripped.lines().collect(ImmutableList.toImmutableList());

                String prefix = textBlockToIndent.getOrDefault(
                        treePath,
                        (lineStartPosition + startColumn + 4 > startPosition ? "" : " ".repeat(4))
                                + " ".repeat(startColumn));
                StringBuilder output = new StringBuilder(initialLines.get(0));
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    output.append(separator);
                    if (!line.isEmpty()) {
                        // Don't add incidental leading whitespace to empty lines
                        output.append(prefix);
                    }
                    if (i == lines.size() - 1) {
                        String withoutDelimiter = line.substring(0, line.length() - TEXT_BLOCK_DELIMITER.length())
                                .stripTrailing();
                        if (!withoutDelimiter.isEmpty()) {
                            output.append(withoutDelimiter)
                                    .append('\\')
                                    .append(separator)
                                    .append(prefix);
                        }
                        // If the trailing line is just """, indenting it more than the prefix of incidental
                        // whitespace has no effect, and results in a javac text-blocks warning that 'trailing
                        // white space will be removed'.
                        output.append(TEXT_BLOCK_DELIMITER);
                    } else {
                        output.append(line);
                    }
                }
                replacements.put(Range.closedOpen(startPosition, endPosition), output.toString());
            }
        }

        /**
         * Pjf specific: When the AST needs to break a concatenation expression ('ONE'+'TWO') or the parameters of a method,
         * the indentation will be set to 8, in which case we need to align the text blocks to the correct indentation.
         * In order to compute the indentation value we need to do:
         * 1. for each textBlock find the enclosing block/parent (only for concat expressions/method invocations)
         * 2. for each parent, find the arguments/concatenated expressions and find the max indentation level
         * (ignore the lines that start with a text block ending)
         * 3. store the mapping between the textBlock and the indentation level computed before.
         */
        private Map<TreePath, String> computeCustomTextBlocksIndent(List<TreePath> textBlocks) {
            Map<Tree, String> parentToIndent = new HashMap<>();
            Map<TreePath, Tree> textBlockToParent = new HashMap<>();
            for (TreePath textBlock : textBlocks) {
                TreePath parentPath = textBlock.getParentPath();
                Tree parent = parentPath.getLeaf();
                if (parent instanceof MethodInvocationTree) {
                    textBlockToParent.put(textBlock, parent);
                    if (parentToIndent.containsKey(parent)) {
                        continue;
                    }
                    List<Tree> allArguments = new ArrayList<>(((JCMethodInvocation) parent).getArguments());
                    parentToIndent.put(
                            parent,
                            // A method can be split in multiple lines (eg. for field access
                            // Class.builder()
                            //      .method("arguments")
                            //      .build()
                            // In this case the parent of the arguments should be the method ("Class.builder().method")
                            // the indentation of the arguments will be relative to the indentation of the last line of
                            // the method name.
                            computePrefixIndentation(
                                    ((JCMethodInvocation) parent).getMethodSelect(), allArguments, false));
                } else if (parent.getKind() == Kind.PLUS) {
                    while (parentPath.getParentPath().getLeaf().getKind() == Kind.PLUS) {
                        parentPath = parentPath.getParentPath();
                    }
                    parent = parentPath.getLeaf();
                    textBlockToParent.put(textBlock, parent);
                    if (parentToIndent.containsKey(parent)) {
                        continue;
                    }
                    parentToIndent.put(parent, computePrefixIndentation(parent, flattenExpressionTree(parent), true));
                }
            }

            return textBlockToParent.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> parentToIndent.get(e.getValue())));
        }

        private String computePrefixIndentation(
                Tree parentPath, List<Tree> childExpressions, boolean shouldUseStartLineParent) {
            int startParentPosition = getStartPosition(parentPath);
            int startParentLine = lineMap.getLineNumber(startParentPosition);
            int endParentPosition = getEndPosition(unit, parentPath);
            int endParentLine = lineMap.getLineNumber(endParentPosition);
            int lineParentStartPosition =
                    lineMap.getStartPosition(shouldUseStartLineParent ? startParentLine : endParentLine);
            int startParentColumn = CharMatcher.whitespace()
                    .negate()
                    .indexIn(input.substring(lineParentStartPosition, endParentPosition));
            int extraIndent = 4;
            RangeSet<Integer> allRanges = TreeRangeSet.create();
            for (Tree expression : childExpressions) {
                int startingPos = getStartPosition(expression);
                int startLine = lineMap.getLineNumber(startingPos);
                int lineStartPosition = lineMap.getStartPosition(startLine);
                int endPos = getEndPosition(unit, expression);
                int endLine = lineMap.getLineNumber(endPos);
                allRanges.add(Range.closed(startLine, endLine));
                int parentLine = shouldUseStartLineParent ? startParentLine : endParentLine;
                // ignore indentation if the current argument is on the same line as the parent
                if (startLine == parentLine) {
                    continue;
                }
                // if this is a line that ends a textBlock (if the line starts with triple quotes & the current tree
                // starts after)
                int startColumn = CharMatcher.whitespace().negate().indexIn(input.substring(lineStartPosition, endPos));
                if (input.startsWith("\"\"\"", lineStartPosition + startColumn)
                        && startingPos != lineStartPosition + startColumn) {
                    continue;
                }

                extraIndent = Math.max(extraIndent, startColumn - startParentColumn);
            }
            return " ".repeat(extraIndent + startParentColumn);
        }

        private void wrapLongStrings(TreeRangeMap<Integer, String> replacements, List<TreePath> longStringLiterals) {
            for (TreePath path : longStringLiterals) {
                // Find the outermost contiguous enclosing concatenation expression
                TreePath enclosing = path;
                while (enclosing.getParentPath().getLeaf().getKind() == Tree.Kind.PLUS) {
                    enclosing = enclosing.getParentPath();
                }
                // Is the literal being wrapped the first in a chain of concatenation expressions?
                // i.e. `ONE + TWO + THREE`
                // We need this information to handle continuation indents.
                AtomicBoolean first = new AtomicBoolean(false);
                // Finds the set of string literals in the concat expression that includes the one that needs
                // to be wrapped.
                List<Tree> flat = flatten(input, unit, path, enclosing, first);

                // ==== START pjf specific ===
                // Walk up as long as parents are on the same line, in order to find the correct
                // startColumn.
                TreePath startingPath = enclosing;
                while (startingPath.getParentPath() != null && onSameLineAsParent(lineMap, startingPath)) {
                    startingPath = startingPath.getParentPath();
                }
                // Zero-indexed start column
                int startColumn = lineMap.getColumnNumber(getStartPosition(flat.get(0))) - 1;
                int fistLineCol = lineMap.getColumnNumber(getStartPosition(startingPath.getLeaf())) - 1;
                // ==== END pjf specific ===

                // Handling leaving trailing non-string tokens at the end of the literal,
                // e.g. the trailing `);` in `foo("...");`.
                @SuppressWarnings("for-rollout:NullAway")
                int end = getEndPosition(unit, getLast(flat));
                int lineEnd = end;
                while (Newlines.hasNewlineAt(input, lineEnd) == -1) {
                    lineEnd++;
                }
                int trailing = lineEnd - end;

                // Get the original source text of the string literals, excluding `"` and `+`.
                ImmutableList<String> components = stringComponents(input, unit, flat);
                replacements.put(
                        Range.closedOpen(getStartPosition(flat.get(0)), getEndPosition(unit, getLast(flat))),
                        reflow(separator, columnLimit, trailing, components, first.get(), startColumn, fistLineCol));
            }
        }

        private static boolean onSameLineAsParent(LineMap lineMap, TreePath path) {
            return lineMap.getLineNumber(getStartPosition(path.getLeaf()))
                    == lineMap.getLineNumber(
                            getStartPosition(path.getParentPath().getLeaf()));
        }
    }

    /**
     * Returns the source text of the given string literal trees, excluding the leading and trailing double-quotes and
     * the `+` operator.
     */
    private static ImmutableList<String> stringComponents(
            String input, JCTree.JCCompilationUnit unit, List<Tree> flat) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        StringBuilder piece = new StringBuilder();
        for (Tree tree : flat) {
            // adjust for leading and trailing double quotes
            String text = input.substring(getStartPosition(tree) + 1, getEndPosition(unit, tree) - 1);
            int start = 0;
            for (int idx = 0; idx < text.length(); idx++) {
                if (CharMatcher.whitespace().matches(text.charAt(idx))) {
                    // continue below
                } else if (hasEscapedWhitespaceAt(text, idx) != -1) {
                    // continue below
                } else if (hasEscapedNewlineAt(text, idx) != -1) {
                    int length;
                    while ((length = hasEscapedNewlineAt(text, idx)) != -1) {
                        idx += length;
                    }
                } else {
                    continue;
                }
                piece.append(text, start, idx);
                result.add(piece.toString());
                piece = new StringBuilder();
                start = idx;
            }
            if (piece.length() > 0) {
                result.add(piece.toString());
                piece = new StringBuilder();
            }
            if (start < text.length()) {
                piece.append(text, start, text.length());
            }
        }
        if (piece.length() > 0) {
            result.add(piece.toString());
        }
        return result.build();
    }

    static int hasEscapedWhitespaceAt(String input, int idx) {
        if (input.startsWith("\\t", idx)) {
            return 2;
        }
        return -1;
    }

    static int hasEscapedNewlineAt(String input, int idx) {
        int offset = 0;
        if (input.startsWith("\\r", idx)) {
            offset += 2;
        }
        if (input.startsWith("\\n", idx)) {
            offset += 2;
        }
        return offset > 0 ? offset : -1;
    }

    /**
     * Reflows the given source text, trying to split on word boundaries.
     *
     * @param separator the line separator
     * @param columnLimit the number of columns to wrap at
     * @param trailing extra space to leave after the last line
     * @param components the text to reflow
     * @param first0 true if the text includes the beginning of its enclosing concat chain, i.e. a
     * @param textStartColumn the column position of the beginning of the original text
     * @param firstLineStartColumn the column where the very first line starts (can be less than textStartColumn if text
     * follows variable declaration)
     */
    private static String reflow(
            String separator,
            int columnLimit,
            int trailing,
            ImmutableList<String> components,
            boolean first0,
            int textStartColumn,
            int firstLineStartColumn) {
        // We have space between the start column and the limit to output the first line.
        // Reserve two spaces for the quotes.
        int width = columnLimit - textStartColumn - 2;
        Deque<String> input = new ArrayDeque<>(components);
        List<String> lines = new ArrayList<>();
        boolean first = first0;
        while (!input.isEmpty()) {
            int length = 0;
            List<String> line = new ArrayList<>();
            if (input.stream().mapToInt(String::length).sum() <= width) {
                width -= trailing;
            }
            while (!input.isEmpty()
                    && (length <= 4 || (length + input.peekFirst().length()) < width)) {
                String text = input.removeFirst();
                line.add(text);
                length += text.length();
                if (text.endsWith("\\n") || text.endsWith("\\r")) {
                    break;
                }
            }
            if (line.isEmpty()) {
                line.add(input.removeFirst());
            }
            // add the split line to the output, and process whatever's left
            lines.add(String.join("", line));
            if (first) {
                // Subsequent lines have a four-space continuation indent and a `+ `.
                width -= 6;
                // Also, switch to firstLineStartColumn in order to account for the fact that continuations
                // should get indented from the beginning of the first line.
                // This is to handle cases like:
                // String foo = "first component"
                //     + "rest";
                width += textStartColumn - firstLineStartColumn;
                first = false;
            }
        }

        return lines.stream()
                .collect(joining(
                        "\"" + separator + " ".repeat(first0 ? firstLineStartColumn + 4 : textStartColumn - 2) + "+ \"",
                        "\"",
                        "\""));
    }

    /**
     * Flattens the given binary expression tree, and extracts the subset that contains the given path and any adjacent
     * nodes that are also string literals.
     */
    private static List<Tree> flatten(
            String input, JCTree.JCCompilationUnit unit, TreePath path, TreePath parent, AtomicBoolean firstInChain) {
        List<Tree> flat = flattenExpressionTree(parent.getLeaf());

        int idx = flat.indexOf(path.getLeaf());
        Verify.verify(idx != -1);

        // walk outwards from the leaf for adjacent string literals to also reflow
        int startIdx = idx;
        int endIdx = idx + 1;
        while (startIdx > 0
                && flat.get(startIdx - 1).getKind() == Tree.Kind.STRING_LITERAL
                && noComments(input, unit, flat.get(startIdx - 1), flat.get(startIdx))) {
            startIdx--;
        }
        while (endIdx < flat.size()
                && flat.get(endIdx).getKind() == Tree.Kind.STRING_LITERAL
                && noComments(input, unit, flat.get(endIdx - 1), flat.get(endIdx))) {
            endIdx++;
        }

        firstInChain.set(startIdx == 0);
        return ImmutableList.copyOf(flat.subList(startIdx, endIdx));
    }

    /**
     * Returns the flatten expression tree with a pre-order traversal
     * @param parent a {@link com.sun.tools.javac.tree.JCTree.JCExpression}
     * @return the list of concatenated parameters in order.
     */
    private static List<Tree> flattenExpressionTree(Tree parent) {
        // flatten the expression tree with a pre-order traversal
        List<Tree> flat = new ArrayList<>();
        ArrayDeque<Tree> todo = new ArrayDeque<>();
        todo.add(parent);
        while (!todo.isEmpty()) {
            Tree first = todo.removeFirst();
            if (first.getKind() == Tree.Kind.PLUS) {
                BinaryTree bt = (BinaryTree) first;
                todo.addFirst(bt.getRightOperand());
                todo.addFirst(bt.getLeftOperand());
            } else {
                flat.add(first);
            }
        }
        return flat;
    }

    private static boolean noComments(String input, JCTree.JCCompilationUnit unit, Tree one, Tree two) {
        return STRING_CONCAT_DELIMITER.matchesAllOf(
                input.subSequence(getEndPosition(unit, one), getStartPosition(two)));
    }

    public static final CharMatcher STRING_CONCAT_DELIMITER =
            CharMatcher.whitespace().or(CharMatcher.anyOf("\"+"));

    private static int getEndPosition(JCTree.JCCompilationUnit unit, Tree tree) {
        return ((JCTree) tree).getEndPosition(unit.endPositions);
    }

    private static int getStartPosition(Tree tree) {
        return ((JCTree) tree).getStartPosition();
    }

    /**
     * Returns true if any lines in the given Java source exceed the column limit or contain text blocks.
     * Keep this method and {@code linesNeedWrapping} in line.
     * */
    private static boolean needWrapping(int columnLimit, String input) {
        // TODO(cushon): consider adding Newlines.lineIterable?
        Iterator<String> it = Newlines.lineIterator(input);
        while (it.hasNext()) {
            String line = it.next();
            if (line.length() > columnLimit || line.contains(TEXT_BLOCK_DELIMITER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the lines containing the {@code initialRangesToChange} in the given Java source exceed the
     * column limit or contain text blocks. Used by the Intellij plugin to check if we need to run StringWrapper.wrap.
     * Keep this method and {@code needWrapping} in line.
     * */
    public static boolean linesNeedWrapping(int columnLimit, String input, RangeSet<Integer> initialRangesToChange) {
        // TODO(cushon): consider adding Newlines.lineIterable?
        RangeSet<Integer> linesToChange = TreeRangeSet.create();
        Iterator<String> it = Newlines.lineIterator(input);
        int i = 0;
        boolean insideTextBlock = false;
        while (it.hasNext()) {
            String line = it.next();
            if (line.length() > columnLimit) {
                linesToChange.add(Range.closedOpen(i, i + 1));
            }
            if (!insideTextBlock && line.contains(TEXT_BLOCK_DELIMITER)) {
                insideTextBlock = true;
                linesToChange.add(Range.closedOpen(i, i + 1));
            } else if (insideTextBlock && line.contains(TEXT_BLOCK_DELIMITER)) {
                insideTextBlock = false;
                linesToChange.add(Range.closedOpen(i, i + 1));
            } else if (insideTextBlock) {
                linesToChange.add(Range.closedOpen(i, i + 1));
            }
            ++i;
        }
        RangeSet<Integer> charRangeToCheck = Utils.lineRangesToCharRanges(input, linesToChange);
        return hasOverlap(initialRangesToChange, charRangeToCheck);
    }

    static boolean hasOverlap(RangeSet<Integer> a, RangeSet<Integer> b) {
        for (Range<Integer> range : a.asRanges()) {
            if (!b.subRangeSet(range).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Parses the given Java source. */
    private static JCTree.JCCompilationUnit parse(String source, boolean allowStringFolding) throws FormatterException {
        Context context = new Context();
        Options.instance(context).put("allowStringFolding", Boolean.toString(allowStringFolding));
        return Formatter.parseJcCompilationUnit(context, source);
    }

    /** Applies replacements to the given string. */
    private static String applyReplacements(String javaInput, TreeRangeMap<Integer, String> replacementMap) {
        // process in descending order so the replacement ranges aren't perturbed if any replacements
        // differ in size from the input
        Map<Range<Integer>, String> ranges = replacementMap.asDescendingMapOfRanges();
        if (ranges.isEmpty()) {
            return javaInput;
        }
        StringBuilder sb = new StringBuilder(javaInput);
        for (Map.Entry<Range<Integer>, String> entry : ranges.entrySet()) {
            Range<Integer> range = entry.getKey();
            sb.replace(range.lowerEndpoint(), range.upperEndpoint(), entry.getValue());
        }
        return sb.toString();
    }

    private StringWrapper() {}
}
