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

import static com.google.common.collect.Iterables.getLast;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.palantir.javaformat.BreakBehaviour;
import com.palantir.javaformat.Breakability;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.Output;
import com.palantir.javaformat.doc.StartsWithBreakVisitor.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** A {@code Level} inside a {@link Doc}. */
public final class Level extends Doc {
  /**
   * How many branches we are allowed to take (i.e how many times we can consider breaking vs not
   * breaking the current level) before we stop branching and always break, which is the
   * google-java-format default behaviour.
   */
  private static final int MAX_BRANCHING_COEFFICIENT = 7;

  private final Indent plusIndent; // The extra indent following breaks.
  private final BreakBehaviour breakBehaviour; // Where to break when we can't fit on one line.
  private final Breakability
      breakabilityIfLastLevel; // If last level, when to break this rather than parent.
  private final List<Doc> docs = new ArrayList<>(); // The elements of the level.

  // State that needs to be preserved between calculating breaks and
  // writing output.
  // TODO(cushon): represent phases as separate immutable data.

  /** True if the entire {@link Level} fits on one line. */
  boolean oneLine = false;

  /**
   * Groups of {@link Doc}s that are children of the current {@link Level}, separated by {@link
   * Break}s.
   */
  List<List<Doc>> splits = new ArrayList<>();

  /** {@link Break}s between {@link Doc}s in the current {@link Level}. */
  List<Break> breaks = new ArrayList<>();

  private Level(
      Indent plusIndent, BreakBehaviour breakBehaviour, Breakability breakabilityIfLastLevel) {
    this.plusIndent = plusIndent;
    this.breakBehaviour = breakBehaviour;
    this.breakabilityIfLastLevel = breakabilityIfLastLevel;
  }

  /**
   * Factory method for {@code Level}s.
   *
   * @param plusIndent the extra indent inside the {@code Level}
   * @param breakBehaviour whether to attempt breaking only the last inner level first, instead of
   *     this level
   * @param breakabilityIfLastLevel if last level, when to break this rather than parent
   * @return the new {@code Level}
   */
  static Level make(
      Indent plusIndent, BreakBehaviour breakBehaviour, Breakability breakabilityIfLastLevel) {
    return new Level(plusIndent, breakBehaviour, breakabilityIfLastLevel);
  }

  /**
   * Add a {@link Doc} to the {@code Level}.
   *
   * @param doc the {@link Doc} to add
   */
  void add(Doc doc) {
    docs.add(doc);
  }

  @Override
  float computeWidth() {
    float thisWidth = 0.0F;
    for (Doc doc : docs) {
      thisWidth += doc.getWidth();
    }
    return thisWidth;
  }

  @Override
  String computeFlat() {
    StringBuilder builder = new StringBuilder();
    for (Doc doc : docs) {
      builder.append(doc.getFlat());
    }
    return builder.toString();
  }

  @Override
  Range<Integer> computeRange() {
    Range<Integer> docRange = EMPTY_RANGE;
    for (Doc doc : docs) {
      docRange = union(docRange, doc.range());
    }
    return docRange;
  }

  @Override
  public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
    float thisWidth = getWidth();
    if (state.column + thisWidth <= maxWidth) {
      oneLine = true;
      // Fix all breaks in this level, recursively.
      ClearBreaksVisitor.INSTANCE.visitLevel(this);
      return state.withColumn(state.column + (int) thisWidth);
    }
    oneLine = false;

    // Attempt 1: try to break the last inner doc if it's a level.
    // No plusIndent since we expect this whole level (except part of the last inner level) to be
    // on the first line.
    if (breakBehaviour == BreakBehaviour.PREFER_BREAKING_LAST_INNER_LEVEL
        && state.branchingCoefficient < MAX_BRANCHING_COEFFICIENT) {
      // Try both breaking and not breaking. Choose the better one based on LOC, preferring
      // breaks if the outcome is the same.

      State state1 = state.withNewBranch();

      State broken =
          computeBroken(commentsHelper, maxWidth, state1.withIndentIncrementedBy(plusIndent));

      Optional<State> lastLevelBroken =
          tryBreakLastLevel(commentsHelper, maxWidth, state1.withNoIndent(), false);

      if (lastLevelBroken.isPresent()) {
        if (lastLevelBroken.get().numLines < broken.numLines) {
          return state.updateAfterLevel(lastLevelBroken.get());
        }
        // Must run computeBroken once again, because our last tryBreakLastLevel run modified
        // mutable state.
        // Therefore just fall through.
      }
    }

    // Attempt 2: break this level.
    State broken =
        computeBroken(commentsHelper, maxWidth, state.withIndentIncrementedBy(plusIndent));

    // But undo the break in a special case, if the inner levels didn't fit on one line.
    // Note: this is currently only used for variable initialisers
    if (breakBehaviour == BreakBehaviour.BREAK_ONLY_IF_INNER_LEVELS_THEN_FIT_ON_ONE_LINE) {
      List<Level> innerLevels = this.docs
          .stream()
          .filter(doc -> doc instanceof Level)
          .map(doc -> ((Level) doc))
          .collect(Collectors.toList());

      boolean anyLevelWasBroken = innerLevels.stream().anyMatch(level -> !level.oneLine);

      boolean prefixFits = false;
      if (anyLevelWasBroken) {
        // Find the first level, skipping empty levels (that contain nothing, or are made up
        // entirely of other empty levels).
        Level firstLevel = innerLevels.stream()
                .filter(doc -> StartsWithBreakVisitor.INSTANCE.visit(doc) != Result.EMPTY)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Levels were broken so expected to find at least a non-empty level"));

        // Add the width of tokens, breaks before the firstLevel. We must always have space for
        // these.
        List<Doc> leadingDocs = docs.subList(0, docs.indexOf(firstLevel));
        float leadingWidth = getWidth(leadingDocs);

        // Potentially add the width of prefixes we want to consider as part of the width that
        // must fit on the same line, so that we don't accidentally break prefixes when we could
        // have avoided doing so.
        leadingWidth += new CountWidthUntilBreakVisitor(maxWidth - state.indent).visit(firstLevel);

        boolean fits = !Float.isInfinite(leadingWidth) && state.column + leadingWidth <= maxWidth;

        if (fits) {
          prefixFits = true;
        }
      }

      // Allow long strings to stay on the same line, expecting that StringWrapper will
      // reflow them later.
      if (prefixFits || isSingleString()) {
        // don't break this level, but preserve indentation
        broken =
            tryToLayOutLevelOnOneLine(
                commentsHelper,
                maxWidth,
                state.withNoIndent().withIndentIncrementedBy(plusIndent));
      }
    }

    return state.updateAfterLevel(broken);
  }

  /**
   * Trick to cooperate with StringWrapper. If the value is a single element (e.g. a String), then
   * allow it to stay on this line, and rely on the StringWrapper to break it accordingly.
   *
   * <p>This is to prevent StringWrapperIntegrationTest#idemponent from breaking.
   */
  private boolean isSingleString() {
    return !this.docs.stream().anyMatch(doc -> doc instanceof Level)
        && this.docs.stream().filter(doc -> doc instanceof Break).count() == 1
        && getFlat().startsWith(" \"");
  }

  private Optional<State> tryBreakLastLevel(
      CommentsHelper commentsHelper, int maxWidth, State state, boolean recursive) {
    if (docs.isEmpty() || !(getLast(docs) instanceof Level)) {
      return Optional.empty();
    }
    Level lastLevel = ((Level) getLast(docs));
    // Only split levels that have declared they want to be split in this way.
    if (lastLevel.breakabilityIfLastLevel == Breakability.NO_PREFERENCE) {
      return Optional.empty();
    }
    // See if we can fill in everything but the lastDoc.
    // This is essentially like a small part of computeBreaks.
    List<Doc> leadingDocs = docs.subList(0, docs.size() - 1);
    float leadingWidth = getWidth(leadingDocs);

    if (state.column + leadingWidth > maxWidth) {
      return Optional.empty();
    }

    // Note: we can't use computeBroken with 'leadingDocs' instead of 'docs', because it assumes
    // _we_ are breaking.
    //       See computeBreakAndSplit -> shouldBreak

    // TODO abstract out
    splitByBreaks(leadingDocs, splits, breaks);

    state = tryToLayOutLevelOnOneLine(commentsHelper, maxWidth, state);
    Preconditions.checkState(
        !state.mustBreak,
        "We messed up, it wants to break a bunch of splits that shouldn't be broken");

    // manually add the last level to the last split
    getLast(splits).add(lastLevel);

    // When recursing into a level, ensure we clear its oneLine.
    // We have to do this because of branching and because oneLine is mutable.
    if (recursive) {
      oneLine = false;
    }

    // Ok now how to handle the last level?
    // There are two options:
    //  * the lastLevel wants to be split, i.e. has Breakability.BREAK_HERE, then we continue
    //  * the lastLevel indicates we should check inside it for a potential split candidate.
    //    In this case, recurse rather than go into computeBreaks.
    if (lastLevel.breakabilityIfLastLevel == Breakability.CHECK_INNER) {
      // Try to fit the entire inner prefix if it's that kind of level.
      if (lastLevel.breakBehaviour != BreakBehaviour.PREFER_BREAKING_LAST_INNER_LEVEL) {
        // We don't know how to fit the inner level on the same line, so bail out.
        return Optional.empty();
      }

      // We want to keep _lastLevel_'s indent, it is an _intermediate_ level.
      state = state.withIndentIncrementedBy(lastLevel.getPlusIndent());

      return lastLevel.tryBreakLastLevel(commentsHelper, maxWidth, state, true);

    } else if (lastLevel.breakabilityIfLastLevel == Breakability.ONLY_IF_FIRST_LEVEL_FITS) {
      // Otherwise, we may be able to check if the first inner level of the lastLevel fits.
      // This is safe because we assume (and check) that a newline comes after it, even though
      // it might be nested somewhere deep in the 2nd level.

      float firstLevelWidth = lastLevel.docs.get(0).getWidth();
      boolean enoughRoom = state.column + firstLevelWidth <= maxWidth;

      // Enforce our assumption.
      if (lastLevel.docs.size() > 1) {
        assertStartsWithBreakOrEmpty(lastLevel.docs.get(1));
      }

      if (!enoughRoom) {
        return Optional.empty();
      }

      // Else, fall back to computeBreaks which will try both with / without break.
    }

    // Note: computeBreaks, not computeBroken, so it can try to do this logic recursively for the
    // lastLevel
    return Optional.of(lastLevel.computeBreaks(commentsHelper, maxWidth, state));
  }

  private static void assertStartsWithBreakOrEmpty(Doc doc) {
    Preconditions.checkState(
        StartsWithBreakVisitor.INSTANCE.visit(doc) != Result.NO,
        "Doc should have started with a break but didn't:\n%s",
        new LevelDelimitedFlatValueDocVisitor().visit(doc));
  }

  /**
   * Mark breaks in this level as not broken, but lay out the inner levels normally, according to
   * their own {@link BreakBehaviour}. The resulting {@link State#mustBreak} will be true if this
   * level did not fit on exactly one line.
   *
   * <p>This relies on {@link #splitByBreaks} having been called beforehand so that {@link #splits}
   * and {@link #breaks} are set.
   */
  private State tryToLayOutLevelOnOneLine(
      CommentsHelper commentsHelper, int maxWidth, State state) {

    for (int i = 0; i < splits.size(); ++i) {
      if (i > 0) {
        state = breaks.get(i - 1).computeBreaks(state, false);
      }

      List<Doc> split = splits.get(i);
      float splitWidth = getWidth(split);
      boolean enoughRoom = state.column + splitWidth <= maxWidth;
      state = computeSplit(commentsHelper, maxWidth, split, state.withMustBreak(false));
      if (!enoughRoom) {
        state = state.withMustBreak(true);
      }
    }
    return state;
  }

  private static void splitByBreaks(List<Doc> docs, List<List<Doc>> splits, List<Break> breaks) {
    splits.clear();
    breaks.clear();
    splits.add(new ArrayList<>());
    for (Doc doc : docs) {
      if (doc instanceof Break) {
        breaks.add((Break) doc);
        splits.add(new ArrayList<>());
      } else {
        getLast(splits).add(doc);
      }
    }
  }

  /** Compute breaks for a {@link Level} that spans multiple lines. */
  private State computeBroken(CommentsHelper commentsHelper, int maxWidth, State state) {
    // TODO abstract out
    splitByBreaks(docs, splits, breaks);

    if (!breaks.isEmpty()) {
      state = state.withBrokenLevel();
    }

    state =
        computeBreakAndSplit(
            commentsHelper, maxWidth, state, /* optBreakDoc= */ Optional.empty(), splits.get(0));

    // Handle following breaks and split.
    for (int i = 0; i < breaks.size(); i++) {
      state =
          computeBreakAndSplit(
              commentsHelper, maxWidth, state, Optional.of(breaks.get(i)), splits.get(i + 1));
    }
    return state;
  }

  /** Lay out a Break-separated group of Docs in the current Level. */
  private static State computeBreakAndSplit(
      CommentsHelper commentsHelper,
      int maxWidth,
      State state,
      Optional<Break> optBreakDoc,
      List<Doc> split) {
    float breakWidth = optBreakDoc.isPresent() ? optBreakDoc.get().getWidth() : 0.0F;
    float splitWidth = getWidth(split);
    boolean shouldBreak =
        (optBreakDoc.isPresent() && optBreakDoc.get().getFillMode() == FillMode.UNIFIED)
            || state.mustBreak
            || state.column + breakWidth + splitWidth > maxWidth;

    if (optBreakDoc.isPresent()) {
      state = optBreakDoc.get().computeBreaks(state, shouldBreak);
    }
    boolean enoughRoom = state.column + splitWidth <= maxWidth;
    state = computeSplit(commentsHelper, maxWidth, split, state.withMustBreak(false));
    if (!enoughRoom) {
      state = state.withMustBreak(true); // Break after, too.
    }
    return state;
  }

  private static State computeSplit(
      CommentsHelper commentsHelper, int maxWidth, List<Doc> docs, State state) {
    for (Doc doc : docs) {
      state = doc.computeBreaks(commentsHelper, maxWidth, state);
    }
    return state;
  }

  @Override
  public void write(Output output) {
    if (oneLine) {
      output.append(getFlat(), range()); // This is defined because width is finite.
    } else {
      writeFilled(output);
    }
  }

  private void writeFilled(Output output) {
    // Handle first split.
    for (Doc doc : splits.get(0)) {
      doc.write(output);
    }
    // Handle following breaks and split.
    for (int i = 0; i < breaks.size(); i++) {
      breaks.get(i).write(output);
      for (Doc doc : splits.get(i + 1)) {
        doc.write(output);
      }
    }
  }

  Indent getPlusIndent() {
    return plusIndent;
  }

  BreakBehaviour getBreakBehaviour() {
    return breakBehaviour;
  }

  List<Doc> getDocs() {
    return docs;
  }

  Breakability getBreakabilityIfLastLevel() {
    return breakabilityIfLastLevel;
  }

  /** An indented representation of this level and all nested levels inside it. */
  String representation() {
    return new LevelDelimitedFlatValueDocVisitor().visit(this);
  }

  /**
   * Get the width of a sequence of {@link Doc}s.
   *
   * @param docs the {@link Doc}s
   * @return the width, or {@code Float.POSITIVE_INFINITY} if any {@link Doc} must be broken
   */
  static float getWidth(List<Doc> docs) {
    float width = 0.0F;
    for (Doc doc : docs) {
      width += doc.getWidth();
    }
    return width;
  }

  private static Range<Integer> union(Range<Integer> x, Range<Integer> y) {
    return x.isEmpty() ? y : y.isEmpty() ? x : x.span(y).canonical(INTEGERS);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("plusIndent", plusIndent)
        .add("breakBehaviour", breakBehaviour)
        .add("preferBreakIfLastLevel", breakabilityIfLastLevel)
        .add("docs", docs)
        .toString();
  }
}
