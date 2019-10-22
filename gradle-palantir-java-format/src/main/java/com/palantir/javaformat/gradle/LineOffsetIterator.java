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

package com.palantir.javaformat.gradle;

import java.util.Iterator;
import java.util.NoSuchElementException;

final class LineOffsetIterator implements Iterator<Integer> {

    private int curr = 0;
    private int idx = 0;
    private final String input;

    public LineOffsetIterator(String input) {
        this.input = input;
    }

    @Override
    public boolean hasNext() {
        return curr != -1;
    }

    @Override
    public Integer next() {
        if (curr == -1) {
            throw new NoSuchElementException();
        }
        int result = curr;
        advance();
        return result;
    }

    private void advance() {
        for (; idx < input.length(); idx++) {
            char c = input.charAt(idx);
            switch (c) {
                case '\r':
                    if (idx + 1 < input.length() && input.charAt(idx + 1) == '\n') {
                        idx++;
                    }
                    // falls through
                case '\n':
                    idx++;
                    curr = idx;
                    return;
                default:
                    break;
            }
        }
        curr = -1;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
