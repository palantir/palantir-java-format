package com.palantir.javaformat;

import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.java.JavaInputAstVisitor;

/**
 * What are the conditions for a level to be partially inlineable. Partial inlining refers to the behaviour of {@link
 * BreakBehaviours#breakOnlyIfInnerLevelsThenFitOnOneLine} where a level is too large to fit on the current line, but a
 * prefix thereof is partially inlined onto the current line.
 *
 * <p>Specifically, when inlining a level with the above behaviour, the partial inlineability of its first
 * <em>child</em> level (and <em>that</em> level's first child, recursively) is queried in order to determine if we need
 * to ensure there's enough room for some additional prefix of that level.
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
 *
 * <p>Note that this works as a mandatory access control. Namely, if it's <em>allowed</em> to partially inline a level,
 * what are the additional conditions that have to be met in order for the inlining to go ahead.
 */
public enum PartialInlineability {
    /**
     * The level may always be partially inlined, regardless of how much space is left on the current line.
     *
     * <p>This is usually only appropriate for levels that start with a direct {@link Break}, as opposed to a Break
     * that's nested inside some other levels.
     */
    ALWAYS_PARTIALLY_INLINEABLE,

    /**
     * Partially inlineable if the <em>first</em> inner level of this level fits on the current line.
     *
     * <p>This assumes that the next Doc after that starts with a {@link Break} (see {@link StartsWithBreakVisitor}) and
     * makes sense in contexts like {@link JavaInputAstVisitor#visitDotWithPrefix} where we want to treat first doc (the
     * longest prefix) as a single entity to be fit onto the same line.
     */
    IF_FIRST_LEVEL_FITS,
}
