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
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
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
    public void testLinesNeedWrapping() throws FormatterException {
        String input =
                """
            class RSLs {
                 String a = ""\"
                 lorem
                   ipsum
                 ""\";
                 String b = ""\"
                 lorem
                 ipsum
                   ""\";
                 String c = ""\"
                 lorem
                 ipsum
                 ""\";
                 String d = ""\"
                 ipsum
                 ""\";
                 String e = ""\"
                 ""\";
                 String f = ""\"
                 ipsum""\";
                 String g = ""\"
                 lorem\\
                 ipsum
                 ""\";
                 String h = ""\"
                 lorem\\
                 ipsum\\
                 ""\";
                 String i = ""\"
                 lorem

                 ipsum
                 ""\";
                 String j = ""\"
                 lorem
                 one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt
                 ipsum
                 ""\";
                 String k = ""\"
                 lorem
                 ipsum
                 ""\";
                 String l = ""\"
                   foo
                 bar
                   baz""\";

                 {
                     f(""\"
                 lorem
                 ipsum
                 ""\", 42);

                     ""\"
                 hello %s
                 ""\".formatted("world");
                     f(/* foo= */ ""\"
                     foo
                     ""\", /* bar= */ ""\"
                     bar
                     ""\");
                     ""\"
                 hello
                 ""\".codePoints().forEach(System.err::println);
                     String s = ""\"
                     foo
                     ""\" + ""\"
                         bar
                         ""\";
                     String notBroken = ""\"
                 foo
                 ""\" + ""\"
                 bar
                 ""\";
                     String working = ""\"
                     foo
                     ""\" + ""\"
                     bar
                     ""\";
                     String u = stringVariableOne
                             + ""\"
                         ...
                         ""\"
                             + stringVariableTwo
                             + ""\"
                                 ...
                                 ""\"
                             + sdklfjslfkjsadlkgjsdklfjsadlkfjsaklfjaskdsfkljsaklfjsadflkjsdafkljasdfklsjfklsajflkasjfsfaaaaaaa
                             + ""\"
                                  my value
                                 ""\";
                 }

                 String x = String.format(""\"
                       this very long string that does something using arguments %s %s %s
                       ""\", "@", "@", "something");

                 {
                     ""\"
                     No tools or answer found in the message. Please try again, following the instructions:\\s

                     %s
                     ""\".someOtherValue(e -> e.getValue().print())
                             .myValue(System.err::println)
                             .formatted(toolFormatter.usage())
                             .myOtherValue()
                             .someOtherValue()
                             .somethingElse();
                 }

                 String y = refactorFromTo(""\"
                     someCode
                     ""\", ""\"
                         otherCode
                         ""\");

                 String z = lotsOfStringParams(""\"
                     aaa
                     ""\", ""\"
                     bbb
                     ""\", ""\"
                     ccc
                     ""\", ""\"
                     ddd
                     ""\", ""\"
                     eee
                     ""\", ""\"
                     fff
                     ""\", ""\"
                     ggg
                     ""\");

                 String w = lotsOfStringParams(
                         ""\"
                                 aaa
                                 ""\", "sdklfjslfkjsadlkgjsdklfjsadlkfjsaklfjaskdsfkljsaklfjsadflkjsdafkljasdfklsjfklsajflkasjfsf", ""\"
                                 bbb
                                 ""\", ""\"
                                 ccc
                                 ""\");
                 String w = lotsOfStringParams(
                         ""\"
                         aaa
                         ""\",
                         "sdklfjslfkjsadlkgjsdklfjsadlkfjsaklfjaskdsfkljsaklfjsadflkjsdafkljasdfklsjfklsajflkasjfsfdasdsadasdsada",
                         ""\"
                         bbb
                         ""\",
                         ""\"
                         ccc
                         ""\");
             }
             """;
        String output =
                """
            class Value {
              String a = \"""
                  hello
                  \""".codePoints();

                String b = getMyValue();

              String c =
                  String.format("This is a String"
                      + " that contains more"
                      + " characters"
                      + " %s", getValue());
            }
            """;

        RangeSet<Integer> fullInput = TreeRangeSet.create();
        fullInput.add(Range.closedOpen(0, input.length()));
        assertThat(StringWrapper.linesNeedWrapping(40, input, fullInput)).isTrue();
        assertThat(StringWrapper.wrap(40, input, Formatter.create())).isEqualTo(output);

        RangeSet<Integer> noStringInput = TreeRangeSet.create();
        int bAssignment = input.indexOf("String b");
        int bEndLine = input.indexOf("\n", bAssignment);
        noStringInput.add(Range.closed(bAssignment, bEndLine));
        assertThat(StringWrapper.linesNeedWrapping(40, input, noStringInput)).isFalse();

        RangeSet<Integer> onlyStringLiteralInput = TreeRangeSet.create();
        onlyStringLiteralInput.add(Range.closedOpen(bEndLine + 1, input.length()));
        assertThat(StringWrapper.linesNeedWrapping(40, input, onlyStringLiteralInput))
                .isTrue();
    }

    private static String lines(String... line) {
        return Joiner.on('\n').join(line) + '\n';
    }
}
