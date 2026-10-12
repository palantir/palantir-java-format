/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.palantir.javaformat.java;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class StringWrapperTest {
    // https://github.com/palantir/palantir-java-format/issues/68
    @Test
    public void joiningWouldChangeEscapes_flagsOctalEscapesThatCanSwallowAnotherDigit() {
        // `\1` then `2` is not the same as `\12`
        assertThat(StringWrapper.joiningWouldChangeEscapes("a\\1", "2b")).isTrue();
        assertThat(StringWrapper.joiningWouldChangeEscapes("\\12", "3")).isTrue();
        assertThat(StringWrapper.joiningWouldChangeEscapes("\\0", "0")).isTrue();
    }

    @Test
    public void joiningWouldChangeEscapes_allowsEverythingElse() {
        // A three digit octal escape is already maximal.
        assertThat(StringWrapper.joiningWouldChangeEscapes("\\377", "7")).isFalse();
        // An escaped backslash means the digits are literal text, not an escape.
        assertThat(StringWrapper.joiningWouldChangeEscapes("back\\\\", "1")).isFalse();
        // Plain trailing digits are not an escape.
        assertThat(StringWrapper.joiningWouldChangeEscapes("12", "3")).isFalse();
        // 8 and 9 are not octal digits.
        assertThat(StringWrapper.joiningWouldChangeEscapes("\\1", "8")).isFalse();
        // Nothing to swallow.
        assertThat(StringWrapper.joiningWouldChangeEscapes("\\1", "")).isFalse();
        assertThat(StringWrapper.joiningWouldChangeEscapes("", "1")).isFalse();
    }

    @Test
    public void mayHaveJoinableLiterals_onlyMatchesAPlusBetweenTwoQuotes() {
        assertThat(StringWrapper.mayHaveJoinableLiterals("String s = \"a\" + \"b\";"))
                .isTrue();
        assertThat(StringWrapper.mayHaveJoinableLiterals("String s = \"a\"+\"b\";"))
                .isTrue();
        // Only one side is a literal, so there is nothing to join.
        assertThat(StringWrapper.mayHaveJoinableLiterals("String s = value + \"b\";"))
                .isFalse();
        assertThat(StringWrapper.mayHaveJoinableLiterals("String s = \"a\" + value;"))
                .isFalse();
        assertThat(StringWrapper.mayHaveJoinableLiterals("int i = one + two;")).isFalse();
        assertThat(StringWrapper.mayHaveJoinableLiterals("")).isFalse();
    }

    @Test
    public void testAwkwardLineEndWrapping() throws Exception {
        String input = lines(
                "class T {",
                // This is a wide line, but has to be split in code because of 100-char limit.
                "  String s = someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() "
                        + "+ \"foo bar foo bar foo bar\";",
                "",
                "  String someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() {",
                "    return null;",
                "  }",
                "}");
        String output = lines(
                "class T {",
                "  String s = someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit()",
                "      + \"foo bar foo bar foo bar\";",
                "",
                "  String someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() {",
                "    return null;",
                "  }",
                "}");

        assertThat(StringWrapper.wrap(100, input, Formatter.create())).isEqualTo(output);
    }

    private static String lines(String... line) {
        return Joiner.on('\n').join(line) + '\n';
    }
}
