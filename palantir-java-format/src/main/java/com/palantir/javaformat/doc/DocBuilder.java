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
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpenOp;
import com.palantir.javaformat.OpsBuilder;
import java.util.ArrayDeque;
import java.util.List;

/** A {@code DocBuilder} converts a sequence of {@link Op}s into a {@link Doc}. */
public final class DocBuilder {
    private final Level base = Level.make(OpenOp.builder().plusIndent(Indent.Const.ZERO).debugName("root").build());
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

    /** Open a new {@link Level}. */
    public void open(OpenOp openOp) {
        Level level = Level.make(openOp);
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
    public Level build() {
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
