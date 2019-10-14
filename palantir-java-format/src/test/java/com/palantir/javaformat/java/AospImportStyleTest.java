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

package com.palantir.javaformat.java;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.palantir.javaformat.jupiter.ParameterizedClass;
import java.util.Arrays;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Tests for import ordering in AOSP style. */
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(ParameterizedClass.class)
public class AospImportStyleTest {

    private final String input;
    private final String reordered;

    public AospImportStyleTest(String input, String reordered) {
        this.input = input;
        this.reordered = reordered;
    }

    @ParameterizedClass.Parameters(name = "{index}: {0}")
    public static Object[][] parameters() {
        // Inputs are provided as three-dimensional arrays. Each element of the outer array is a test
        // case. It consists of two arrays of lines. The first array of lines is the test input, and
        // the second one is the expected output. If the second array has a single element starting
        // with !! then it is expected that ImportOrderer will throw a FormatterException with that
        // message.
        //
        // If a line ends with \ then we remove the \ and don't append a \n. That allows us to check
        // some parsing edge cases.
        String[][][] inputsOutputs = {
            // Capital letter before lowercase
            {
                {
                    "package foo;", "", "import android.abC.Bar;", "import android.abc.Bar;", "public class Blim {}",
                },
                {
                    "package foo;",
                    "",
                    "import android.abC.Bar;",
                    "import android.abc.Bar;",
                    "",
                    "public class Blim {}",
                }
            },
            // Blank line between "com.android" and "com.anythingelse"
            {
                {
                    "package foo;", "", "import com.android.Bar;", "import com.google.Bar;", "public class Blim {}",
                },
                {
                    "package foo;",
                    "",
                    "import com.android.Bar;",
                    "",
                    "import com.google.Bar;",
                    "",
                    "public class Blim {}",
                }
            },
            // Rough ordering -- statics, android, third party, then java, with blank lines between
            // major groupings
            {
                {
                    "package foo;",
                    "",
                    "import static net.Bar.baz;",
                    "import static org.junit.Bar.baz;",
                    "import static com.google.Bar.baz;",
                    "import static java.lang.Bar.baz;",
                    "import static junit.Bar.baz;",
                    "import static javax.annotation.Bar.baz;",
                    "import static android.Bar.baz;",
                    "import net.Bar;",
                    "import org.junit.Bar;",
                    "import com.google.Bar;",
                    "import java.lang.Bar;",
                    "import junit.Bar;",
                    "import javax.annotation.Bar;",
                    "import android.Bar;",
                    "public class Blim {}",
                },
                {
                    "package foo;",
                    "",
                    "import static android.Bar.baz;",
                    "",
                    "import static com.google.Bar.baz;",
                    "",
                    "import static junit.Bar.baz;",
                    "",
                    "import static net.Bar.baz;",
                    "",
                    "import static org.junit.Bar.baz;",
                    "",
                    "import static java.lang.Bar.baz;",
                    "",
                    "import static javax.annotation.Bar.baz;",
                    "",
                    "import android.Bar;",
                    "",
                    "import com.google.Bar;",
                    "",
                    "import junit.Bar;",
                    "",
                    "import net.Bar;",
                    "",
                    "import org.junit.Bar;",
                    "",
                    "import java.lang.Bar;",
                    "",
                    "import javax.annotation.Bar;",
                    "",
                    "public class Blim {}",
                }
            },
            {
                {
                    "package foo;",
                    "",
                    "import static java.first.Bar.baz;",
                    "import static com.second.Bar.baz;",
                    "import com.first.Bar;",
                    "import static android.second.Bar.baz;",
                    "import dalvik.first.Bar;",
                    "import static dalvik.first.Bar.baz;",
                    "import static androidx.second.Bar.baz;",
                    "import java.second.Bar;",
                    "import static com.android.second.Bar.baz;",
                    "import static net.first.Bar.baz;",
                    "import gov.second.Bar;",
                    "import junit.second.Bar;",
                    "import static libcore.second.Bar.baz;",
                    "import static java.second.Bar.baz;",
                    "import static net.second.Bar.baz;",
                    "import static org.first.Bar.baz;",
                    "import static dalvik.second.Bar.baz;",
                    "import javax.first.Bar;",
                    "import static javax.second.Bar.baz;",
                    "import android.first.Bar;",
                    "import android.second.Bar;",
                    "import static javax.first.Bar.baz;",
                    "import androidx.first.Bar;",
                    "import static androidx.first.Bar.baz;",
                    "import androidx.second.Bar;",
                    "import com.android.first.Bar;",
                    "import gov.first.Bar;",
                    "import com.android.second.Bar;",
                    "import dalvik.second.Bar;",
                    "import static org.second.Bar.baz;",
                    "import net.first.Bar;",
                    "import libcore.second.Bar;",
                    "import static android.first.Bar.baz;",
                    "import com.second.Bar;",
                    "import static gov.second.Bar.baz;",
                    "import static gov.first.Bar.baz;",
                    "import static junit.first.Bar.baz;",
                    "import libcore.first.Bar;",
                    "import junit.first.Bar;",
                    "import javax.second.Bar;",
                    "import static libcore.first.Bar.baz;",
                    "import net.second.Bar;",
                    "import static com.first.Bar.baz;",
                    "import org.second.Bar;",
                    "import static junit.second.Bar.baz;",
                    "import java.first.Bar;",
                    "import org.first.Bar;",
                    "import static com.android.first.Bar.baz;",
                    "public class Blim {}",
                },
                {
                    "package foo;", //
                    "",
                    "import static android.first.Bar.baz;",
                    "import static android.second.Bar.baz;",
                    "",
                    "import static androidx.first.Bar.baz;",
                    "import static androidx.second.Bar.baz;",
                    "",
                    "import static com.android.first.Bar.baz;",
                    "import static com.android.second.Bar.baz;",
                    "",
                    "import static dalvik.first.Bar.baz;",
                    "import static dalvik.second.Bar.baz;",
                    "",
                    "import static libcore.first.Bar.baz;",
                    "import static libcore.second.Bar.baz;",
                    "",
                    "import static com.first.Bar.baz;",
                    "import static com.second.Bar.baz;",
                    "",
                    "import static gov.first.Bar.baz;",
                    "import static gov.second.Bar.baz;",
                    "",
                    "import static junit.first.Bar.baz;",
                    "import static junit.second.Bar.baz;",
                    "",
                    "import static net.first.Bar.baz;",
                    "import static net.second.Bar.baz;",
                    "",
                    "import static org.first.Bar.baz;",
                    "import static org.second.Bar.baz;",
                    "",
                    "import static java.first.Bar.baz;",
                    "import static java.second.Bar.baz;",
                    "",
                    "import static javax.first.Bar.baz;",
                    "import static javax.second.Bar.baz;",
                    "",
                    "import android.first.Bar;",
                    "import android.second.Bar;",
                    "",
                    "import androidx.first.Bar;",
                    "import androidx.second.Bar;",
                    "",
                    "import com.android.first.Bar;",
                    "import com.android.second.Bar;",
                    "",
                    "import dalvik.first.Bar;",
                    "import dalvik.second.Bar;",
                    "",
                    "import libcore.first.Bar;",
                    "import libcore.second.Bar;",
                    "",
                    "import com.first.Bar;",
                    "import com.second.Bar;",
                    "",
                    "import gov.first.Bar;",
                    "import gov.second.Bar;",
                    "",
                    "import junit.first.Bar;",
                    "import junit.second.Bar;",
                    "",
                    "import net.first.Bar;",
                    "import net.second.Bar;",
                    "",
                    "import org.first.Bar;",
                    "import org.second.Bar;",
                    "",
                    "import java.first.Bar;",
                    "import java.second.Bar;",
                    "",
                    "import javax.first.Bar;",
                    "import javax.second.Bar;",
                    "",
                    "public class Blim {}",
                },
            },
        };
        ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
        Arrays.stream(inputsOutputs).forEach(input -> builder.add(ImportOrdererUtils.createRow(input)));
        return builder.build().toArray(new Object[][] {});
    }

    @TestTemplate
    public void reorder() throws FormatterException {
        try {
            String output = ImportOrderer.reorderImports(input, JavaFormatterOptions.Style.AOSP);
            assertWithMessage("Expected exception").that(reordered).doesNotMatch("^!!");
            assertWithMessage(input).that(output).isEqualTo(reordered);
        } catch (FormatterException e) {
            if (!reordered.startsWith("!!")) {
                throw e;
            }
            assertThat(reordered).endsWith("\n");
            assertThat(e).hasMessageThat().isEqualTo("error: " + reordered.substring(2, reordered.length() - 1));
        }
    }
}
