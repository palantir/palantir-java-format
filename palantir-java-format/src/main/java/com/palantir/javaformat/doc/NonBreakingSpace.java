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
import com.google.common.collect.Range;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.CommentsHelper;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.Output;

/** A Leaf node in a {@link Doc} for a non-breaking space. */
@Immutable
public final class NonBreakingSpace extends Doc implements Op {
    private static final NonBreakingSpace SPACE = new NonBreakingSpace();

    private NonBreakingSpace() {}

    public static NonBreakingSpace make() {
        return SPACE;
    }

    @Override
    public void add(DocBuilder builder) {
        builder.add(this);
    }

    @Override
    protected float computeWidth() {
        return 1.0F;
    }

    @Override
    protected String computeFlat() {
        return " ";
    }

    @Override
    protected Range<Integer> computeRange() {
        return Doc.EMPTY_RANGE;
    }

    @Override
    public State computeBreaks(
            CommentsHelper commentsHelper, int maxWidth, State state, Obs.ExplorationNode observationNode) {
        return state.withColumn(state.column() + 1);
    }

    @Override
    public void write(State state, Output output) {
        output.append(state, " ", range());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
