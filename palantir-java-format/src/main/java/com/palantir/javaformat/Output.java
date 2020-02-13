/*
 * (c) Copyright 2015 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Range;
import com.palantir.javaformat.doc.State;

/** An output from the formatter. */
public abstract class Output extends InputOutput {
    /**
     * Indent by outputting {@code indent} spaces.
     *
     * @param indent the current indent
     */
    public abstract void indent(int indent);

    /**
     * Output a string.
     *
     * @param state
     * @param text the string
     * @param range the {@link Range} corresponding to the string
     */
    public abstract void append(State state, String text, Range<Integer> range);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("super", super.toString()).toString();
    }
}
