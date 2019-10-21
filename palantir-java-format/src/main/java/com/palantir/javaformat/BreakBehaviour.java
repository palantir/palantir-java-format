package com.palantir.javaformat;

import org.derive4j.ArgOption;
import org.derive4j.Data;

@Data(arguments = ArgOption.checkedNotNull)
public abstract class BreakBehaviour {
    public interface Cases<R> {

        R breakThisLevel();

        R preferBreakingLastInnerLevel(KeepIndentWhenInlined keepIndentWhenInlined);

        R breakOnlyIfInnerLevelsThenFitOnOneLine(KeepIndentWhenInlined keepIndentWhenInlined);
    }

    public enum KeepIndentWhenInlined {
        NO,
        YES,
    }

    public abstract <R> R match(Cases<R> cases);

    /** For {@link com.palantir.javaformat.doc.LevelDelimitedFlatValueDocVisitor}. */
    @Override
    public abstract String toString();
}
