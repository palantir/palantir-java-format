package com.palantir.javaformat;

import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.Level;
import org.derive4j.ArgOption;
import org.derive4j.Data;

@Data(arguments = ArgOption.checkedNotNull)
public abstract class BreakBehaviour {
    public interface Cases<R> {

        R breakThisLevel();

        /**
         * If the last level is breakable, prefer breaking it if it will keep the rest of this level on line line.
         *
         * @param keepIndentWhenInlined whether to keep this level's indent when inlined as a recursive level (when
         *     reached via a previous `preferBreakingLastInnerLevel` whose breakability was {@link
         *     LastLevelBreakability#CHECK_INNER})
         */
        R preferBreakingLastInnerLevel(boolean keepIndentWhenInlined);

        /**
         * Break if by doing so all inner levels then fit on a single line. However, don't break if we can fit in the
         * {@link Doc docs} up to the first break (which might be nested inside the next doc if it's a {@link Level}),
         * in order to prevent exceeding the maxLength accidentally.
         */
        R breakOnlyIfInnerLevelsThenFitOnOneLine(boolean keepIndentWhenInlined);
    }

    public abstract <R> R match(Cases<R> cases);

    /** For {@link com.palantir.javaformat.doc.LevelDelimitedFlatValueDocVisitor}. */
    @Override
    public abstract String toString();
}
