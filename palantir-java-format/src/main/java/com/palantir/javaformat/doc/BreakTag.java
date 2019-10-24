package com.palantir.javaformat.doc;

import com.google.errorprone.annotations.Immutable;

/**
 * Unique identifier for a break. A BreakTag can correspond to one or more {@link Break breaks}, and the state of the
 * BreakTag is determined by whether any of the breaks were taken. Then, conditional structures like {@link
 * com.palantir.javaformat.OpsBuilder.BlankLineWanted.ConditionalBlankLine} and {@link
 * com.palantir.javaformat.Indent.If} behave differently based on whether this BreakTag was 'broken' or not.
 *
 * @see State#wasBreakTaken
 */
@Immutable
public final class BreakTag extends HasUniqueId {}
