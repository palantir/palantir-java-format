/*
 * Copyright 2015 Google Inc.
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

package com.palantir.javaformat;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.DocBuilder;
import com.palantir.javaformat.doc.HasUniqueId;
import com.palantir.javaformat.doc.Level;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * An {@code OpenOp} opens a level. It is an {@link Op} in the sequence of {@link Op}s generated by {@link OpsBuilder}.
 * When the sequence is turned into a {@link Doc} by {@link DocBuilder}, {@link Input.Tok}s delimited by
 * {@code OpenOp}-{@link CloseOp} pairs turn into nested {@link Level}s.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableOpenOp.class)
public abstract class OpenOp extends HasUniqueId implements Op {
    /** The extra indent inside this level. */
    public abstract Indent plusIndent();

    /**
     * When this level doesn't fit on one line, controls whether this level is to be broken (its breaks taken) or
     * partially inlined onto the current line.
     */
    @Default
    public BreakBehaviour breakBehaviour() {
        return BreakBehaviours.breakThisLevel();
    }

    /** If it's the last level of its parent, when to inline this level rather than break the parent. */
    @Default
    public LastLevelBreakability breakabilityIfLastLevel() {
        return LastLevelBreakability.ABORT;
    }

    @Default
    public PartialInlineability partialInlineability() {
        return PartialInlineability.ALWAYS_PARTIALLY_INLINEABLE;
    }

    /**
     * A level is "simple" if it doesn't have multiple parameters (in the case of a method call), or multiple chained
     * method calls.
     *
     * <p>This is used to poison the ability to partially inline method arguments down the line if a parent level was
     * too complicated, so that you can't end up with this:
     *
     * <pre>
     * method(arg1, arg2, arg3.foo().stream()
     *         .filter(...)
     *         .map(...));
     * </pre>
     *
     * or
     *
     * <pre>
     * log.info("Message", exception, SafeArg.of(
     *         "foo", foo);
     * </pre>
     *
     * But you can still get this (see test B20128760):
     *
     * <pre>
     * Stream<ItemKey> itemIdsStream = stream(members).flatMap(m -> m.getFieldValues().entrySet().stream()
     *         .filter(...)
     *         .map(...));
     * </pre>
     *
     * or this:
     *
     * <pre>
     * method(anotherMethod(arg3.foo().stream()
     *         .filter(...)
     *         .map(...)));
     * </pre>
     *
     * or this:
     *
     * <pre>
     * method(anotherMethod(
     *         ...)); // long arguments
     * </pre>
     */
    @Default
    public Complexity complexity() {
        return Complexity.SIMPLE_IF_CURRENT_INLINE_CHAIN_IS_SIMPLE;
    }

    public enum Complexity {
        /**
         * If the current chain of levels being inlined so far have all been simple, then the chain so far is simple
         * too.
         */
        SIMPLE_IF_CURRENT_INLINE_CHAIN_IS_SIMPLE,
        /** The inline chain so far is always considered to be simple. */
        FORCE_SIMPLE,
        /** The inline chain is complex, which will cause certain inlinings to not be considered. */
        COMPLEX,
    }

    public abstract Optional<String> debugName();

    /** Custom max column limit that contents of this level <em>before the last break</em> may not exceed. */
    public abstract OptionalInt columnLimitBeforeLastBreak();

    /**
     * Make an ordinary {@code OpenOp}.
     *
     * @see #builder()
     */
    public static Op make(Indent plusIndent) {
        return builder().plusIndent(plusIndent).build();
    }

    @Override
    public void add(DocBuilder builder) {
        builder.open(this);
    }

    /** @see ImmutableOpenOp.Builder#Builder() */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ImmutableOpenOp.Builder {
        public Builder isSimple(boolean isSimple) {
            return complexity(isSimple ? Complexity.SIMPLE_IF_CURRENT_INLINE_CHAIN_IS_SIMPLE : Complexity.COMPLEX);
        }
    }
}
