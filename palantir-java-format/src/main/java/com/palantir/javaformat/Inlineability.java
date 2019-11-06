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
     * The level is not partially inlineable. Appropriate for levels which contain multiple arguments.
     *
     * <p>It also applies recursively on an entire chain of inlined levels (that'd all end up on the same line except
     * the very last inner level) and poisons the ability to inline single method arguments, so that you can't end up
     * with this:
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
    NOT_INLINEABLE_AND_POISON_FUTURE_INLINING_ON_THIS_LINE,
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
