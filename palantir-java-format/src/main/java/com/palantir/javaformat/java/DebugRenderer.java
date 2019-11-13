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

import com.google.common.collect.ImmutableList;
import com.palantir.javaformat.Op;
import com.palantir.javaformat.OpsBuilder;
import com.palantir.javaformat.doc.Comment;

public class DebugRenderer {

    public static void render(JavaInput javaInput, OpsBuilder.OpsOutput opsOutput) {
        // new File("/Users/dfox/Desktop/output.html");

        System.out.println(javaInput.getText());

        ImmutableList<Op> ops = opsOutput.ops();
        for (Op op : ops) {
            if (op instanceof Comment) {
                System.out.println(op);
            }
        }
    }
}
