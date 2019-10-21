package com.palantir.javaformat;

import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.java.JavaInputAstVisitor;

/**
 * How to decide whether to break the last inner level ("this level") of a parent level with {@link
 * BreakBehaviour.Cases#preferBreakingLastInnerLevel}.
 */
public enum Breakability {
    /** Default behaviour - it cannot be broken. */
    NO_PREFERENCE,
    /**
     * Unconditionally allow breaking this level. This should only be used when you know that the first non-Level {@link
     * Doc} inside this level, if you flatten it, is a {@link Break}.
     */
    BREAK_HERE,
    /**
     * Delegate to the {@link Breakability} of _this_ level's last inner level. Typically, this will be true if this
     * level is not immediately followed by a break (see StartsWithBreakVisitor). Behaves the same as {@link
     * #NO_PREFERENCE} if this level is not {@link BreakBehaviour.Cases#preferBreakingLastInnerLevel}.
     */
    CHECK_INNER,
    /**
     * Check whether the <i>first</i> inner level can fit on the same line. This assumes that the next Doc after that
     * starts with a {@link Break} (see {@link StartsWithBreakVisitor}) and makes sense in contexts like {@link
     * JavaInputAstVisitor#visitDotWithPrefix} where we want to treat first doc (the longest prefix) as a single entity
     * to be fit onto the same line.
     */
    ONLY_IF_FIRST_LEVEL_FITS,
    ;
}
