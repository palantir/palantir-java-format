package com.palantir.javaformat;

import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.Level;

/** How to decide where to break when a level can't fit on a single line. */
public enum BreakBehaviour {
    /** Always break this level. */
    BREAK_THIS_LEVEL,
    /** If the last level is breakable, prefer breaking it if it will keep the rest of this level on line line. */
    PREFER_BREAKING_LAST_INNER_LEVEL,
    /**
     * Break if by doing so all inner levels then fit on a single line. However, don't break if we can fit in the {@link
     * Doc docs} up to the first break (which might be nested inside the next doc if it's a {@link Level}), in order to
     * prevent exceeding the maxLength accidentally.
     */
    BREAK_ONLY_IF_INNER_LEVELS_THEN_FIT_ON_ONE_LINE,
}
