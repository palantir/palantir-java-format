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
import com.palantir.javaformat.BreakBehaviour;
import com.palantir.javaformat.BreakBehaviours;
import com.palantir.javaformat.Indent;
import com.palantir.javaformat.Inlineability;
import com.palantir.javaformat.LastLevelBreakability;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpsBuilder;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

/** A {@code DocBuilder} converts a sequence of {@link Op}s into a {@link Doc}. */
public final class DocBuilder {
    private final Level base = Level.make(
            Indent.Const.ZERO,
            BreakBehaviours.breakThisLevel(),
            LastLevelBreakability.ABORT,
            Optional.of("root"),
            Inlineability.ALWAYS_INLINEABLE);
    private final ArrayDeque<Level> stack = new ArrayDeque<>();

    /**
     * A possibly earlier {@link Level} for appending text, à la Philip Wadler.
     *
     * <p>Processing {@link Doc}s presents a subtle problem. Suppose we have a {@link Doc} for to an assignment node,
     * {@code a = b}, with an optional {@link Break} following the {@code =}. Suppose we have 5 characters to write it,
     * so that we think we don't need the break. Unfortunately, this {@link Doc} lies in an expression statement {@link
     * Doc} for the statement {@code a = b;} and this statement does not fit in 3 characters. This is why many
     * formatters sometimes emit lines that are too long, or cheat by using a narrower line length to avoid such
     * problems.
     *
     * <p>One solution to this problem is not to decide whether a {@link Level} should be broken until later (in this
     * case, after the semicolon has been seen). A simpler approach is to rewrite the {@link Doc} as here, so that the
     * semicolon moves inside the inner {@link Doc}, and we can decide whether to break that {@link Doc} without seeing
     * later text.
     */
    private Level appendLevel = base;

    /** Start to build a {@code DocBuilder}. */
    public DocBuilder() {
        stack.addLast(base);
    }

    /**
     * Add a list of {@link Op}s to the {@link OpsBuilder}.
     *
     * @param ops the {@link Op}s
     * @return the {@link OpsBuilder}
     */
    public DocBuilder withOps(List<Op> ops) {
        for (Op op : ops) {
            op.add(this); // These operations call the operations below to build the doc.
        }
        return this;
    }

    /**
     * Open a new {@link Level}.
     *
     * @param plusIndent the extra indent for the {@link Level}
     * @param breakBehaviour how to decide whether to break this level or not
     * @param breakabilityIfLastLevel if last level, when to break this rather than parent
     */
    public void open(
            Indent plusIndent,
            BreakBehaviour breakBehaviour,
            LastLevelBreakability breakabilityIfLastLevel,
            Optional<String> debugName,
            Inlineability inlineability) {
        Level level = Level.make(plusIndent, breakBehaviour, breakabilityIfLastLevel, debugName, inlineability);
        stack.addLast(level);
    }

    /** Close the current {@link Level}. */
    public void close() {
        Level top = stack.removeLast();
        stack.peekLast().add(top);
    }

    /**
     * Add a {@link Doc} to the current {@link Level}.
     *
     * @param doc the {@link Doc}
     */
    void add(Doc doc) {
        appendLevel.add(doc);
    }

    /**
     * Add a {@link Break} to the current {@link Level}.
     *
     * @param breakDoc the {@link Break}
     */
    void breakDoc(Break breakDoc) {
        appendLevel = stack.peekLast();
        appendLevel.add(breakDoc);
    }

    /**
     * Return the {@link Doc}.
     *
     * @return the {@link Doc}
     */
    public Doc build() {
        return base;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("base", base)
                .add("stack", stack)
                .add("appendLevel", appendLevel)
                .toString();
    }
}
