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

import com.google.common.base.MoreObjects;
import com.palantir.javaformat.Indent;

/** State for writing. */
public final class State {
  /** Last indent that was actually taken. */
  final int lastIndent;
  /** Next indent, if the level is about to be broken. */
  final int indent;

  final int column;
  final boolean mustBreak;
  /** Counts how many lines a particular formatting took. */
  final int numLines;
  /**
   * Counts how many times reached a branch, where multiple formattings would be considered.
   * Expected runtime is exponential in this number.
   *
   * @see State#withNewBranch()
   */
  final int branchingCoefficient;

  State(
      int lastIndent,
      int indent,
      int column,
      boolean mustBreak,
      int numLines,
      int branchingCoefficient) {
    this.lastIndent = lastIndent;
    this.indent = indent;
    this.column = column;
    this.mustBreak = mustBreak;
    this.numLines = numLines;
    this.branchingCoefficient = branchingCoefficient;
  }

  public State(int indent0, int column0) {
    this(indent0, indent0, column0, false, 0, 0);
  }

  /**
   * Increases the indent by {@link Indent#eval} of the {@code plusIndent}, and sets {@code
   * mustBreak} to false. Does not commit to the indent just yet though, so lastIndent stays the
   * same.
   */
  State withIndentIncrementedBy(Indent plusIndent) {
    return new State(
        lastIndent, indent + plusIndent.eval(), column, false, numLines, branchingCoefficient);
  }

  /** Reset any accumulated indent to the same value as {@code lastIndent}. */
  State withNoIndent() {
    return new State(lastIndent, lastIndent, column, false, numLines, branchingCoefficient);
  }

  /** The current level is being broken and it has breaks in it. Commit to the indent. */
  State withBrokenLevel() {
    return new State(indent, indent, column, mustBreak, numLines, branchingCoefficient);
  }

  State withBreak(Break brk) {
    int newColumn = Math.max(indent + brk.evalPlusIndent(), 0);
    // lastIndent = indent -- we've proven that we wrote some stuff at the new 'indent' so commit
    // to it
    return new State(indent, indent, newColumn, mustBreak, numLines + 1, branchingCoefficient);
  }

  State updateAfterLevel(State state) {
    return new State(
        lastIndent, indent, state.column, mustBreak, state.numLines, branchingCoefficient);
  }

  State addNewLines(int extraNewlines) {
    return new State(
        lastIndent, indent, column, mustBreak, numLines + extraNewlines, branchingCoefficient);
  }

  State withColumn(int column) {
    return new State(lastIndent, indent, column, mustBreak, numLines, branchingCoefficient);
  }

  State withMustBreak(boolean mustBreak) {
    return new State(lastIndent, indent, column, mustBreak, numLines, branchingCoefficient);
  }

  State withNewBranch() {
    return new State(lastIndent, indent, column, mustBreak, numLines, branchingCoefficient + 1);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("lastIndent", lastIndent)
        .add("indent", indent)
        .add("column", column)
        .add("mustBreak", mustBreak)
        .toString();
  }
}
