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
package com.palantir.javaformat;

import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.Doc;

/**
 * How to decide whether to break the last inner level ("this level") of a parent level with
 * {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel}.
 */
public enum LastLevelBreakability {
    /**
     * Default behaviour. When processing a {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel} chain, if we've
     * arrived at a level with this breakability, then we should abort the chain.
     */
    ABORT,
    /**
     * Unconditionally allow ending an inline chain at this level, after which this level may be broken as usual, or a
     * prefix thereof could be inlined further (if it has the appropriate break behaviour of
     * {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel}). This should only be used when you know that the first
     * non-Level {@link Doc} inside this level, if you flatten it, is a {@link Break}.
     */
    ACCEPT_INLINE_CHAIN,
    /**
     * Delegate to the {@link LastLevelBreakability} of _this_ level's last inner level. Typically, this will be true if
     * this level is not immediately followed by a break (see StartsWithBreakVisitor). Behaves the same as
     * {@link #ABORT} if this level is not {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel}.
     */
    CHECK_INNER,
    /**
     * Behaves like {@link #ACCEPT_INLINE_CHAIN} if the current inlined levels are all <em>simple</em> (according to
     * {@link OpenOp#complexity()}), otherwise behave like {@link #CHECK_INNER}.
     */
    ACCEPT_INLINE_CHAIN_IF_SIMPLE_OTHERWISE_CHECK_INNER,
    ;
}
