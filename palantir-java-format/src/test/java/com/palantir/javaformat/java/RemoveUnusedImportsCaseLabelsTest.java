/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
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
import static com.palantir.javaformat.java.RemoveUnusedImports.removeUnusedImports;

import com.google.common.base.Joiner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

final class RemoveUnusedImportsCaseLabelsTest {

    @Test
    void preserveTypesInCaseLabels() throws FormatterException {
        Assumptions.assumeTrue(Formatter.getRuntimeVersion() >= 17, "Not running on jdk 17 or later");
        String input = Joiner.on('\n')
                .join(
                        "package example;",
                        "import example.model.SealedInterface;",
                        "import example.model.TypeA;",
                        "import example.model.TypeB;",
                        "public class Main {",
                        "    public void apply(SealedInterface sealedInterface) {",
                        "        switch(sealedInterface) {",
                        "            case TypeA a -> System.out.println(\"A!\");",
                        "            case TypeB b -> System.out.println(\"B!\");",
                        "        }",
                        "    }",
                        "}");
        assertThat(removeUnusedImports(input)).isEqualTo(input);
    }
}
