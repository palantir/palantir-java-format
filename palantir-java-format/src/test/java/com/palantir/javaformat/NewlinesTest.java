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

package com.palantir.javaformat;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class NewlinesTest {
    @Test
    public void offsets() {
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\nbar\n")))
                .containsExactly(0, 4, 8);
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\nbar")))
                .containsExactly(0, 4);

        Truth.assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\rbar\r")))
                .containsExactly(0, 4, 8);
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\rbar")))
                .containsExactly(0, 4);

        Truth.assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\r\nbar\r\n")))
                .containsExactly(0, 5, 10);
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\r\nbar")))
                .containsExactly(0, 5);
    }

    @Test
    public void lines() {
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\nbar\n")))
                .containsExactly("foo\n", "bar\n");
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\nbar")))
                .containsExactly("foo\n", "bar");

        Truth.assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\rbar\r")))
                .containsExactly("foo\r", "bar\r");
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\rbar")))
                .containsExactly("foo\r", "bar");

        Truth.assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\r\nbar\r\n")))
                .containsExactly("foo\r\n", "bar\r\n");
        Truth.assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\r\nbar")))
                .containsExactly("foo\r\n", "bar");
    }

    @Test
    public void terminalOffset() {
        Iterator<Integer> it = Newlines.lineOffsetIterator("foo\nbar\n");
        it.next();
        it.next();
        it.next();
        try {
            it.next();
            Assert.fail();
        } catch (NoSuchElementException e) {
            // expected
        }

        it = Newlines.lineOffsetIterator("foo\nbar");
        it.next();
        it.next();
        try {
            it.next();
            Assert.fail();
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void terminalLine() {
        Iterator<String> it = Newlines.lineIterator("foo\nbar\n");
        it.next();
        it.next();
        try {
            it.next();
            Assert.fail();
        } catch (NoSuchElementException e) {
            // expected
        }

        it = Newlines.lineIterator("foo\nbar");
        it.next();
        it.next();
        try {
            it.next();
            Assert.fail();
        } catch (NoSuchElementException e) {
            // expected
        }
    }
}
