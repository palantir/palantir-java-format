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

public final class ImportOrdererUtils {
    public static Object[] createRow(String[][] inputAndOutput) {
        assertThat(inputAndOutput).hasLength(2);
        String[] input = inputAndOutput[0];
        String[] output = inputAndOutput[1];
        if (output.length == 0) {
            output = input;
        }
        Object[] row = {
            Joiner.on('\n').join(input) + '\n', //
            Joiner.on('\n').join(output) + '\n',
        };
        // If a line ends with \ then we remove the \ and don't append a \n. That allows us to check
        // some parsing edge cases.
        row[0] = ((String) row[0]).replace("\\\n", "");
        return row;
    }

    private ImportOrdererUtils() {}
}
