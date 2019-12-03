package com.palantir.javaformat;

import com.palantir.javaformat.doc.State;

public enum InlineIndent {
    AS_USUAL,
    /** Resets the accumulated {@link State#indent()} and adds this level's own {@link OpenOp#plusIndent()}. */
    RESET_AND_ADD_OWN
}
