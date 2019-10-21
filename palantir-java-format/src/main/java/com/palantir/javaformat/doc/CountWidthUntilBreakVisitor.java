/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.javaformat.doc;

import com.google.common.base.Preconditions;
import com.palantir.javaformat.LastLevelBreakability;
import com.palantir.javaformat.doc.StartsWithBreakVisitor.Result;
import java.util.List;
import java.util.OptionalInt;

/** Count the width of code we definitely to try fitting on a single line. */
class CountWidthUntilBreakVisitor implements DocVisitor<Float> {
    private final int availableWidth;

    /**
     * @param availableWidth the max width we will accept a suffix can take before ignoring it / trying to split it
     *     anyway.
     */
    public CountWidthUntilBreakVisitor(int availableWidth) {
        this.availableWidth = availableWidth;
    }

    @Override
    public Float visitSpace(Space doc) {
        return doc.getWidth();
    }

    @Override
    public Float visitTok(Tok doc) {
        return doc.getWidth();
    }

    @Override
    public Float visitToken(Token doc) {
        return doc.getWidth();
    }

    @Override
    public Float visitBreak(Break doc) {
        return doc.getWidth();
    }

    @Override
    public Float visitLevel(Level level) {
        if (level.getBreakabilityIfLastLevel() == LastLevelBreakability.ONLY_IF_FIRST_LEVEL_FITS
                // If this prefix wouldn't fit on a new line (within the availableWidth), then don't
                // consider it at all, because there's no point, it would always be broken.
                && level.getDocs().get(0).getWidth() <= availableWidth) {
            // We will incorporate only the length of this prefix, which is the first level.
            return level.getDocs().get(0).getWidth();
        }
        // Otherwise, try to drill down into the first level that's not empty.
        OptionalInt found = getFirstNonEmptyLevel(level.getDocs());
        if (found.isPresent()) {
            return visit(level.getDocs().get(found.getAsInt()));
        }
        // Otherwise, assert that we encountered a break and move on.
        Preconditions.checkState(
                StartsWithBreakVisitor.INSTANCE.visit(level) == Result.YES,
                "Didn't find expected break at the beginning of level.\n%s",
                level.representation());
        return 0f;
    }

    /**
     * Gets the index of the first {@link Level} that is not empty. If other kinds of {@link Doc}s are encountered,
     * returns {@link OptionalInt#empty}.
     */
    private static OptionalInt getFirstNonEmptyLevel(List<Doc> docs) {
        int idx = 0;
        for (Doc doc : docs) {
            if (!(doc instanceof Level)) {
                break;
            }
            if (StartsWithBreakVisitor.INSTANCE.visit(doc) != Result.EMPTY) {
                return OptionalInt.of(idx);
            }
            idx++;
        }
        return OptionalInt.empty();
    }
}
