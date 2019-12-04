package com.palantir.javaformat;

import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.java.JavaInputAstVisitor;

/**
 * What are the conditions for the prefix of a level to be inlineable, i.e. "writeable onto the current line without
 * breaking first, even if other tokens have been written on the current line already". This enum also decides what
 * prefix of this level we should definitely fit in order for the level to be inlineable.
 *
 * <p>Note that this is distinct from whether it's <em>allowed</em> to inline the level (controlled by {@link
 * LastLevelBreakability}).
 */
public enum Inlineability {
    /**
     * The level may always be partially inlined, regardless of how much space is left on the current line. Partial
     * inlining refers to the behaviour of {@link BreakBehaviours#breakOnlyIfInnerLevelsThenFitOnOneLine} where a level
     * is too large to fit on the current line, but a prefix thereof is partially inlined onto the current line.
     *
     * <p>This is usually only appropriate for levels that start with a direct {@link Break}, as opposed to a Break
     * that's nested inside some other levels.
     */
    MAY_FOLLOW_PARTIALLY_INLINED_LEVEL,

    /**
     * Partially inlineable if the <em>first</em> inner level of this level fits on the current line.
     *
     * <p>This assumes that the next Doc after that starts with a {@link Break} (see {@link StartsWithBreakVisitor}) and
     * makes sense in contexts like {@link JavaInputAstVisitor#visitDotWithPrefix} where we want to treat first doc (the
     * longest prefix) as a single entity to be fit onto the same line.
     *
     * <p>The reason for this is to prevent degenerate formattings like
     *
     * <pre>
     * Object foo = someSuperLongMethod(some |
     *         .fully                        |
     *         .qualified                    |
     *         .ClassName                    |
     *         .doSomething());              |
     * </pre>
     *
     * and instead prefer breaking earlier to keep the prefix on the same line, like:
     *
     * <pre>
     * Object foo = someSuperLongMethod(     |
     *         some.fully.qualified.ClassName|
     *                 .doSomething());      |
     * </pre>
     */
    IF_FIRST_LEVEL_FITS,
    /** Level is not inlineable. */
    NOT_INLINEABLE,
}
