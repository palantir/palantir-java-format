package com.palantir.javaformat;

import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.Doc;

/**
 * How to decide whether to break the last inner level ("this level") of a parent level with {@link
 * BreakBehaviour.Cases#preferBreakingLastInnerLevel}.
 */
public enum LastLevelBreakability {
    /**
     * Default behaviour. When processing a {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel} chain, if we've
     * arrived at a last level with this breakability, then we should abort the chain.
     */
    ABORT,
    /**
     * Unconditionally allow breaking this level. This should only be used when you know that the first non-Level {@link
     * Doc} inside this level, if you flatten it, is a {@link Break}.
     */
    BREAK_HERE,
    /**
     * Delegate to the {@link LastLevelBreakability} of _this_ level's last inner level. Typically, this will be true if
     * this level is not immediately followed by a break (see StartsWithBreakVisitor). Behaves the same as {@link
     * #ABORT} if this level is not {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel}.
     */
    CHECK_INNER,
    ;
}
