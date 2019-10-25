package com.palantir.javaformat;

import com.palantir.javaformat.doc.Break;
import com.palantir.javaformat.java.JavaInputAstVisitor;

/**
 * What are the conditions for a level to be inlineable.
 *
 * <p>Note that this is distinct from whether it's <em>allowed</em> to inline the level (controlled by {@link
 * LastLevelBreakability}).
 */
public enum Inlineability {
    /**
     * The level is always inlineable. This is usually only appropriate for levels that start with a direct {@link
     * Break}, as opposed to a Break that's nested inside some other levels.
     */
    ALWAYS_INLINEABLE,

    /**
     * Inlineable if the <em>first</em> inner level of this level fits on the current line.
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
}
