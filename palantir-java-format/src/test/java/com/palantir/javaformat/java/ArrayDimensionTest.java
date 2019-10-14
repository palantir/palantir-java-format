/*
 * Copyright 2016 Google Inc.
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
import com.google.common.base.Splitter;
import com.palantir.javaformat.jupiter.ParameterizedClass;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Tests for array dimension handling, especially mixed array notation and type annotations on dimensions. */
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(ParameterizedClass.class)
public class ArrayDimensionTest {
    @ParameterizedClass.Parameters
    public static Object[][] parameters() {
        return new Object[][] {
            // mixed array notation multi-variable declarations
            new String[] {"int a[], b @B [], c @B [][] @C [];"},
            new String[] {"int a @A [], b @B [], c @B [] @C [];"},
            new String[] {"int a[] @A [], b @B [], c @B [] @C [];"},
            new String[] {"int a, b @B [], c @B [] @C [];"},
            new String[] {"int @A [] a, b @B [], c @B [] @C [];"},
            new String[] {"int @A [] a = {}, b @B [] = {{}}, c @B [] @C [] = {{{}}};"},
            // mixed array notation methods
            new String[] {"int[][][][][] argh()[] @A @B [] @C @D [][] {}"},
            new String[] {"int[][] @T [] @U [] @V @W [][][] argh() @A @B [] @C @D [] {}"},
            new String[] {"int e1() @A [] {}"},
            new String[] {"int f1()[] @A [] {}"},
            new String[] {"int g1() @A [] @B [] {}"},
            new String[] {"int h1() @A @B [] @C @B [] {}"},
            new String[] {"int[] e2() @A [] {}"},
            new String[] {"int @X [] f2()[] @A [] {}"},
            new String[] {"int[] g2() @A [] @B [] {}"},
            new String[] {"int @X [] h2() @A @B [] @C @B [] {}"},
            new String[] {"@X int[] e3() @A [] {}"},
            new String[] {"@X int @Y [] f3()[] @A [] {}"},
            new String[] {"@X int @Y [] g3() @A [] @B [] {}"},
            new String[] {"@X int[] h3() @A @B [] @C @B [] {}"},
            // mixed array notation single-variable declarations
            new String[] {"int[] e2() @A [] {}"},
            new String[] {"int @I [] f2()[] @A [] {}"},
            new String[] {"int[] @J [] g2() @A [] @B [] {}"},
            new String[] {"int @I [] @J [] h2() @A @B [] @C @B [] {}"},
            new String[] {"int a1[];"},
            new String[] {"int b1 @A [];"},
            new String[] {"int c1[] @A [];"},
            new String[] {"int d1 @A [] @B [];"},
            new String[] {"int[] a2[];"},
            new String[] {"int @A [] b2 @B [];"},
            new String[] {"int[] c2[] @A [];"},
            new String[] {"int @A [] d2 @B [] @C [];"},
            // array dimension expressions
            new String[] {"int[][] a0 = new @A int[0];"},
            new String[] {"int[][] a1 = new int @A [0] @B [];"},
            new String[] {"int[][] a2 = new int[0] @A [] @B [];"},
            new String[] {"int[][] a4 = new int[0] @A [][] @B [];"},
            // nested array type uses
            new String[] {"List<int @A [] @B []> xs;"},
            new String[] {"List<int[] @A [][] @B []> xs;"},
        };
    }

    private final String input;

    public ArrayDimensionTest(String input) {
        this.input = input;
    }

    @TestTemplate
    public void format() throws Exception {
        String source = "class T {" + input + "}";
        String formatted = new Formatter().formatSource(source);
        String statement = formatted.substring("class T {".length(), formatted.length() - "}\n".length());
        // ignore line breaks after declaration-style annotations
        statement = Joiner.on(' ').join(Splitter.on('\n').omitEmptyStrings().trimResults().split(statement));
        assertThat(statement).isEqualTo(input);
    }
}
