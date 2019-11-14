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

import com.google.common.base.Suppliers;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Input;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.Output;

/**
 * {@link com.palantir.javaformat.java.JavaInputAstVisitor JavaInputAstVisitor} outputs a sequence of {@link Op}s using
 * {@link OpsBuilder}. This linear sequence is then transformed by {@link DocBuilder} into a tree-structured {@code
 * Doc}. The top-level {@code Doc} is a {@link Level}, which contains a sequence of {@code Doc}s, including other {@link
 * Level}s. Leaf {@code Doc}s are {@link Token}s, representing language-level tokens; {@link Comment}s, which may also
 * represent non-token {@link Input.Tok}s, including comments and other white-space; {@link NonBreakingSpace}s, representing single
 * spaces; and {@link Break}s, which represent optional line-breaks.
 */
public abstract class Doc extends HasUniqueId {
    static final Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);
    static final DiscreteDomain<Integer> INTEGERS = DiscreteDomain.integers();

    private final ImmutableSupplier<Float> memoizedWidth = Suppliers.memoize(this::computeWidth)::get;
    private final ImmutableSupplier<String> memoizedFlat = Suppliers.memoize(this::computeFlat)::get;
    private final ImmutableSupplier<Range<Integer>> memoizedRange = Suppliers.memoize(this::computeRange)::get;

    /**
     * Return the width of a {@code Doc}, or {@code Float.POSITIVE_INFINITY} if it must be broken.
     *
     * @return the width
     */
    final float getWidth() {
        return memoizedWidth.get();
    }

    /**
     * Return a {@code Doc}'s flat-string value; not defined (and never called) if the (@code Doc} contains forced
     * breaks.
     *
     * @return the flat-string value
     */
    public final String getFlat() {
        return memoizedFlat.get();
    }

    final Range<Integer> range() {
        return memoizedRange.get();
    }

    /**
     * Compute the {@code Doc}'s width.
     *
     * @return the width, or {@code Float.POSITIVE_INFINITY} if it must be broken
     */
    protected abstract float computeWidth();

    /**
     * Compute the {@code Doc}'s flat value. Not defined (and never called) if contains forced breaks.
     *
     * @return the flat value
     */
    protected abstract String computeFlat();

    /**
     * Compute the {@code Doc}'s {@link Range} of {@link Input.Token}s.
     *
     * @return the {@link Range}
     */
    protected abstract Range<Integer> computeRange();

    /**
     * Make breaking decisions for a {@code Doc}.
     *
     * @param maxWidth the maximum line width
     * @param state the current output state
     * @return the new output state
     */
    public abstract State computeBreaks(
            CommentsHelper commentsHelper,
            int maxWidth,
            State state,
            ObservationNode observer);

    /** Write a {@code Doc} to an {@link Output}, after breaking decisions have been made. */
    public abstract void write(State state, Output output);
}
