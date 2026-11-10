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
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class StringWrapperTest {
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

    @Test
    public void testEscapeSequencesAreNotSplit() throws Exception {
        // The source text of string literal characters and escape sequences, including escaped backslashes followed
        // by characters that would otherwise form escapes, and octal and Unicode escapes.
        String[] atoms = {
            "a",
            "bb",
            " ",
            "\\\\",
            "\\t",
            "\\n",
            "\\r\\n",
            "\\\"",
            "\\\\t",
            "\\\\n",
            "\\\\\\t",
            "\\0",
            "\\12",
            "\\377",
            "\\s",
            "\\u0041",
            "\\\\u0041"
        };
        Random random = new Random(42);
        Formatter formatter = Formatter.create();
        for (int i = 0; i < 200; i++) {
            StringBuilder literal = new StringBuilder();
            while (literal.length() < 150) {
                literal.append(atoms[random.nextInt(atoms.length)]);
            }
            String input = "class T {\n  String s = \"" + literal + "\";\n}\n";
            // StringWrapper checks that its output parses and has the same AST as its input.
            StringWrapper.wrap(40, formatter.formatSource(input), formatter);
        }
    }

    private static String lines(String... line) {
        return Joiner.on('\n').join(line) + '\n';
    }
}
