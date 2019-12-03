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
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.palantir.javaformat.BreakBehaviour;
import com.palantir.javaformat.BreakBehaviours;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.LastLevelBreakability;
import com.palantir.javaformat.OpenOp;
import com.palantir.javaformat.Output;
import com.palantir.javaformat.doc.Obs.Exploration;
import com.palantir.javaformat.doc.Obs.ExplorationNode;
import com.palantir.javaformat.doc.StartsWithBreakVisitor.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.immutables.value.Value;

/** A {@code Level} inside a {@link Doc}. */
public final class Level extends Doc {
    /**
     * How many branches we are allowed to take (i.e how many times we can consider breaking vs not breaking the current
     * level) before we stop branching and always break, which is the google-java-format default behaviour.
     */
    private static final int MAX_BRANCHING_COEFFICIENT = 20;

    private static final Collector<Level, ?, Optional<Level>> GET_LAST_COLLECTOR = Collectors.reducing((u, v) -> v);

    private final List<Doc> docs = new ArrayList<>(); // The elements of the level.
    private final ImmutableSupplier<SplitsBreaks> memoizedSplitsBreaks =
            Suppliers.memoize(() -> splitByBreaks(docs))::get;
    /** The immutable characteristics of this level determined before the level contents are available. */
    private final OpenOp openOp;

    private Level(OpenOp openOp) {
        this.openOp = openOp;
    }

    /** Factory method for {@code Level}s. */
    static Level make(OpenOp openOp) {
        return new Level(openOp);
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
    protected float computeWidth() {
        float thisWidth = 0.0F;
        for (Doc doc : docs) {
            thisWidth += doc.getWidth();
        }
        return thisWidth;
    }

    @Override
    protected String computeFlat() {
        StringBuilder builder = new StringBuilder();
        for (Doc doc : docs) {
            builder.append(doc.getFlat());
        }
        return builder.toString();
    }

    @Override
    protected Range<Integer> computeRange() {
        Range<Integer> docRange = EMPTY_RANGE;
        for (Doc doc : docs) {
            docRange = union(docRange, doc.range());
        }
        return docRange;
    }

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state, Obs.ExplorationNode observer) {
        return tryToFitOnOneLine(maxWidth, state).orElseGet(() -> {
            Obs.LevelNode childLevel = observer.newChildNode(this, state);
            State newState = getBreakBehaviour().match(new BreakImpl(commentsHelper, maxWidth, state, childLevel));

            return childLevel.finishLevel(state.updateAfterLevel(newState));
        });
    }

    /**
     * Try to fit this level onto one line. If this returns empty, then the level will be broken in some way that is
     * dictated by the {@link #getBreakBehaviour()}.
     */
    private Optional<State> tryToFitOnOneLine(int maxWidth, State state) {
        if (getColumnLimitBeforeLastBreak().isPresent()) {
            float width = 0.0f;
            float widthBeforeLastBreak = 0.0f;
            for (Doc doc : docs) {
                if (doc instanceof Break) {
                    widthBeforeLastBreak = width;
                }
                width += doc.getWidth();
            }
            // Make an additional check that widthBeforeLastBreak fits in the column limit
            if (state.column() + widthBeforeLastBreak > getColumnLimitBeforeLastBreak().getAsInt()) {
                return Optional.empty();
            }
        }

        // Check that the entirety of this level fits on the current line.
        float thisWidth = getWidth();
        if (state.column() + thisWidth <= maxWidth) {
            return Optional.of(
                    state.withColumn(state.column() + (int) thisWidth)
                            .withLevelState(this, ImmutableLevelState.of(true)));
        }
        return Optional.empty();
    }

    class BreakImpl implements BreakBehaviour.Cases<State> {
        private final CommentsHelper commentsHelper;
        private final int maxWidth;
        private final State state;
        private final Obs.LevelNode levelNode;

        public BreakImpl(CommentsHelper commentsHelper, int maxWidth, State state, Obs.LevelNode levelNode) {
            this.commentsHelper = commentsHelper;
            this.maxWidth = maxWidth;
            this.state = state;
            this.levelNode = levelNode;
        }

        private State breakNormally(State state, ExplorationNode explorationNode) {
            return computeBroken(
                    commentsHelper, maxWidth, state.withIndentIncrementedBy(getPlusIndent()), explorationNode);
        }

        @Override
        public State breakThisLevel() {
            return levelNode
                    .explore("breakThisLevel", state, explorationNode -> breakNormally(state, explorationNode))
                    .markAccepted();
        }

        @Override
        public State preferBreakingLastInnerLevel(boolean _keepIndentWhenInlined) {
            // Try both breaking and not breaking. Choose the better one based on LOC, preferring
            // breaks if the outcome is the same.
            State state = this.state.withNewBranch();

            Obs.Exploration broken = levelNode.explore(
                    "breaking normally", this.state, (explorationNode) -> breakNormally(this.state, explorationNode));

            if (state.branchingCoefficient() < MAX_BRANCHING_COEFFICIENT) {
                State state1 = state.withNoIndent();
                Optional<Obs.Exploration> lastLevelBroken = levelNode.maybeExplore(
                        "tryBreakLastLevel", state1, (explorationNode) ->
                                tryBreakLastLevel(commentsHelper, maxWidth, state1, explorationNode));

                if (lastLevelBroken.isPresent()) {
                    if (lastLevelBroken.get().state().numLines() < broken.state().numLines()) {
                        return lastLevelBroken.get().markAccepted();
                    }
                }
            }
            return broken.markAccepted();
        }

        @Override
        public State breakOnlyIfInnerLevelsThenFitOnOneLine(boolean keepIndentWhenInlined) {
            State stateForBroken = this.state.withIndentIncrementedBy(getPlusIndent());
            Exploration broken = levelNode.explore("breaking normally", stateForBroken, explorationNode ->
                    computeBroken(commentsHelper, maxWidth, stateForBroken, explorationNode));

            Optional<Obs.Exploration> maybeInlined = levelNode.maybeExplore(
                    "handleBreakOnlyIfInnerLevelsThenFitOnOneLine", state, (explorationNode) ->
                            handleBreakOnlyIfInnerLevelsThenFitOnOneLine(
                                    commentsHelper,
                                    maxWidth,
                                    this.state,
                                    broken.state(),
                                    keepIndentWhenInlined,
                                    explorationNode));

            if (maybeInlined.isPresent()) {
                return maybeInlined.get().markAccepted();
            } else {
                return broken.markAccepted();
            }
        }
    }

    private Optional<State> handleBreakOnlyIfInnerLevelsThenFitOnOneLine(
            CommentsHelper commentsHelper,
            int maxWidth,
            State state,
            State brokenState,
            boolean keepIndent,
            Obs.ExplorationNode explorationNode) {
        List<Level> innerLevels = this.docs.stream()
                .filter(doc -> doc instanceof Level)
                .map(doc -> ((Level) doc))
                .collect(Collectors.toList());

        boolean anyLevelWasBroken = innerLevels.stream().anyMatch(level -> !brokenState.isOneLine(level));

        boolean prefixFits = false;
        if (anyLevelWasBroken) {
            // Find the last level, skipping empty levels (that contain nothing, or are made up
            // entirely of other empty levels).

            // Last level because there might be other in-between levels after the initial break like `new
            // int[]
            // {`, and we want to skip those.
            Level lastLevel = innerLevels.stream()
                    .filter(doc -> StartsWithBreakVisitor.INSTANCE.visit(doc) != Result.EMPTY)
                    .collect(GET_LAST_COLLECTOR)
                    .orElseThrow(() -> new IllegalStateException(
                            "Levels were broken so expected to find at least a non-empty level"));

            // Add the width of tokens, breaks before the lastLevel. We must always have space for
            // these.
            List<Doc> leadingDocs = docs.subList(0, docs.indexOf(lastLevel));
            float leadingWidth = getWidth(leadingDocs);

            // Potentially add the width of prefixes we want to consider as part of the width that
            // must fit on the same line, so that we don't accidentally break prefixes when we could
            // have avoided doing so.
            leadingWidth += new CountWidthUntilBreakVisitor(maxWidth - state.indent()).visit(lastLevel);

            boolean fits = !Float.isInfinite(leadingWidth) && state.column() + leadingWidth <= maxWidth;

            if (fits) {
                prefixFits = true;
            }
        }

        if (prefixFits) {
            State newState = state.withNoIndent();
            if (keepIndent) {
                newState = newState.withIndentIncrementedBy(getPlusIndent());
            }
            return Optional.of(
                    tryToLayOutLevelOnOneLine(
                            commentsHelper, maxWidth, newState, memoizedSplitsBreaks.get(), explorationNode));
        }
        return Optional.empty();
    }

    private Optional<State> tryBreakLastLevel(
            CommentsHelper commentsHelper, int maxWidth, State state, Obs.ExplorationNode explorationNode) {
        if (docs.isEmpty() || !(getLast(docs) instanceof Level)) {
            return Optional.empty();
        }
        Level lastLevel = ((Level) getLast(docs));
        // Only split levels that have declared they want to be split in this way.
        if (lastLevel.getBreakabilityIfLastLevel() == LastLevelBreakability.ABORT) {
            return Optional.empty();
        }
        // See if we can fill in everything but the lastDoc.
        // This is essentially like a small part of computeBreaks.
        List<Doc> leadingDocs = docs.subList(0, docs.size() - 1);
        float leadingWidth = getWidth(leadingDocs);

        if (state.column() + leadingWidth > maxWidth) {
            return Optional.empty();
        }

        // Note: we can't use computeBroken with 'leadingDocs' instead of 'docs', because it assumes
        // _we_ are breaking.
        //       See computeBreakAndSplit -> shouldBreak

        SplitsBreaks prefixSplitsBreaks = splitByBreaks(leadingDocs);

        State state1 = tryToLayOutLevelOnOneLine(commentsHelper, maxWidth, state, prefixSplitsBreaks, explorationNode);
        // If a break was still forced somehow even though we could fit the leadingWidth, then abort.
        // This could happen if inner levels have set a `columnLimitBeforeLastBreak` or something like that.
        if (state1.numLines() != state.numLines()) {
            return Optional.empty();
        }

        // Ok now how to handle the last level?
        // There are two options:
        //  * the lastLevel wants to be split, i.e. has Breakability.BREAK_HERE, then we continue
        //  * the lastLevel indicates we should check inside it for a potential split candidate.
        //    In this case, recurse rather than go into computeBreaks.
        if (lastLevel.getBreakabilityIfLastLevel() == LastLevelBreakability.CHECK_INNER) {
            // Try to fit the entire inner prefix if it's that kind of level.
            return BreakBehaviours.caseOf(lastLevel.getBreakBehaviour())
                    .preferBreakingLastInnerLevel(keepIndentWhenInlined -> {
                        State state2 = state1;
                        if (keepIndentWhenInlined) {
                            state2 = state2.withIndentIncrementedBy(lastLevel.getPlusIndent());
                        }
                        State state3 = state2;
                        return explorationNode
                                .newChildNode(lastLevel, state2)
                                .maybeExplore("recurse into inner tryBreakLastLevel", state3, exp ->
                                        lastLevel.tryBreakLastLevel(commentsHelper, maxWidth, state3, exp))
                                .map(expl -> expl.markAccepted()); // collapse??
                    })
                    // We don't know how to fit the inner level on the same line, so bail out.
                    .otherwise_(Optional.empty());

        } else if (lastLevel.getBreakabilityIfLastLevel() == LastLevelBreakability.ONLY_IF_FIRST_LEVEL_FITS) {
            // Otherwise, we may be able to check if the first inner level of the lastLevel fits.
            // This is safe because we assume (and check) that a newline comes after it, even though
            // it might be nested somewhere deep in the 2nd level.

            float firstLevelWidth = lastLevel.docs.get(0).getWidth();
            boolean enoughRoom = state1.column() + firstLevelWidth <= maxWidth;

            // Enforce our assumption.
            if (lastLevel.docs.size() > 1) {
                assertStartsWithBreakOrEmpty(state, lastLevel.docs.get(1));
            }

            if (!enoughRoom) {
                return Optional.empty();
            }

            // Else, fall back to computeBreaks which will try both with / without break.
        }

        // Note: computeBreaks, not computeBroken, so it can try to do this logic recursively for the
        // lastLevel
        return Optional.of(
                explorationNode
                        .newChildNode(lastLevel, state1)
                        .explore("end tryBreakLastLevel chain", state1, exp ->
                                lastLevel.computeBreaks(commentsHelper, maxWidth, state1, exp))
                        .markAccepted());
    }

    private static void assertStartsWithBreakOrEmpty(State state, Doc doc) {
        Preconditions.checkState(
                StartsWithBreakVisitor.INSTANCE.visit(doc) != Result.NO,
                "Doc should have started with a break but didn't:\n%s",
                new LevelDelimitedFlatValueDocVisitor(state).visit(doc));
    }

    /**
     * Mark breaks in this level as not broken, but lay out the inner levels normally, according to their own {@link
     * BreakBehaviour}. The resulting {@link State#mustBreak} will be true if this level did not fit on exactly one
     * line.
     */
    private State tryToLayOutLevelOnOneLine(
            CommentsHelper commentsHelper,
            int maxWidth,
            State state,
            SplitsBreaks splitsBreaks,
            Obs.ExplorationNode explorationNode) {

        for (int i = 0; i < splitsBreaks.splits().size(); ++i) {
            if (i > 0) {
                state = splitsBreaks.breaks().get(i - 1).computeBreaks(state, false);
            }

            List<Doc> split = splitsBreaks.splits().get(i);
            float splitWidth = getWidth(split);
            boolean enoughRoom = state.column() + splitWidth <= maxWidth;
            state = computeSplit(commentsHelper, maxWidth, split, state.withMustBreak(false), explorationNode);
            if (!enoughRoom) {
                state = state.withMustBreak(true);
            }
        }
        return state;
    }

    private static SplitsBreaks splitByBreaks(List<Doc> docs) {
        ImmutableSplitsBreaks.Builder builder = ImmutableSplitsBreaks.builder();
        ImmutableList.Builder<Doc> currentSplit = ImmutableList.builder();
        for (Doc doc : docs) {
            if (doc instanceof Break) {
                builder.addSplits(currentSplit.build());
                currentSplit = ImmutableList.builder();
                builder.addBreaks((Break) doc);
            } else {
                currentSplit.add(doc);
            }
        }
        builder.addSplits(currentSplit.build());
        return builder.build();
    }

    /** Compute breaks for a {@link Level} that spans multiple lines. */
    private State computeBroken(
            CommentsHelper commentsHelper, int maxWidth, State state, Obs.ExplorationNode explorationNode) {
        SplitsBreaks splitsBreaks = memoizedSplitsBreaks.get();

        if (!splitsBreaks.breaks().isEmpty()) {
            state = state.withBrokenLevel();
        }

        ImmutableList<Doc> splitDocs = splitsBreaks.splits().get(0);
        state = computeBreakAndSplit(
                commentsHelper, maxWidth, state, /* optBreakDoc= */ Optional.empty(), splitDocs, explorationNode);

        // Handle following breaks and split.
        for (int i = 0; i < splitsBreaks.breaks().size(); i++) {
            state = computeBreakAndSplit(
                    commentsHelper,
                    maxWidth,
                    state,
                    Optional.of(splitsBreaks.breaks().get(i)),
                    splitsBreaks.splits().get(i + 1),
                    explorationNode);
        }
        return state;
    }

    /** Lay out a Break-separated group of Docs in the current Level. */
    private static State computeBreakAndSplit(
            CommentsHelper commentsHelper,
            int maxWidth,
            State state,
            Optional<Break> optBreakDoc,
            List<Doc> split,
            Obs.ExplorationNode explorationNode) {
        float breakWidth = optBreakDoc.isPresent() ? optBreakDoc.get().getWidth() : 0.0F;
        float splitWidth = getWidth(split);
        boolean shouldBreak = (optBreakDoc.isPresent() && optBreakDoc.get().fillMode() == FillMode.UNIFIED)
                || state.mustBreak()
                || state.column() + breakWidth + splitWidth > maxWidth;

        if (optBreakDoc.isPresent()) {
            state = optBreakDoc.get().computeBreaks(state, shouldBreak);
        }
        boolean enoughRoom = state.column() + splitWidth <= maxWidth;
        state = computeSplit(commentsHelper, maxWidth, split, state.withMustBreak(false), explorationNode);
        if (!enoughRoom) {
            state = state.withMustBreak(true); // Break after, too.
        }
        return state;
    }

    private static State computeSplit(
            CommentsHelper commentsHelper,
            int maxWidth,
            List<Doc> docs,
            State state,
            Obs.ExplorationNode explorationNode) {
        for (Doc doc : docs) {
            state = doc.computeBreaks(commentsHelper, maxWidth, state, explorationNode);
        }
        return state;
    }

    @Override
    public void write(State state, Output output) {
        if (state.isOneLine(this)) {
            output.append(state, getFlat(), range()); // This is defined because width is finite.
        } else {
            writeFilled(state, output);
        }
    }

    private void writeFilled(State state, Output output) {
        SplitsBreaks splitsBreaks = memoizedSplitsBreaks.get();
        // Handle first split.
        for (Doc doc : splitsBreaks.splits().get(0)) {
            doc.write(state, output);
        }
        // Handle following breaks and split.
        for (int i = 0; i < splitsBreaks.breaks().size(); i++) {
            splitsBreaks.breaks().get(i).write(state, output);
            for (Doc doc : splitsBreaks.splits().get(i + 1)) {
                doc.write(state, output);
            }
        }
    }

    Indent getPlusIndent() {
        return openOp.plusIndent();
    }

    BreakBehaviour getBreakBehaviour() {
        return openOp.breakBehaviour();
    }

    List<Doc> getDocs() {
        return docs;
    }

    LastLevelBreakability getBreakabilityIfLastLevel() {
        return openOp.breakabilityIfLastLevel();
    }

    public Optional<String> getDebugName() {
        return openOp.debugName();
    }

    OpenOp getOpenOp() {
        return openOp;
    }

    public OptionalInt getColumnLimitBeforeLastBreak() {
        return openOp.columnLimitBeforeLastBreak();
    }

    /** An indented representation of this level and all nested levels inside it. */
    public String representation(State state) {
        return new LevelDelimitedFlatValueDocVisitor(state).visit(this);
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
                .add("debugName", getDebugName())
                .add("plusIndent", getPlusIndent())
                .add("breakBehaviour", getBreakBehaviour())
                .add("breakabilityIfLastLevel", getBreakabilityIfLastLevel())
                .toString();
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    interface SplitsBreaks {
        /** Groups of {@link Doc}s that are children of the current {@link Level}, separated by {@link Break}s. */
        ImmutableList<ImmutableList<Doc>> splits();
        /** {@link Break}s between {@link Doc}s in the current {@link Level}. */
        ImmutableList<Break> breaks();
    }
}
