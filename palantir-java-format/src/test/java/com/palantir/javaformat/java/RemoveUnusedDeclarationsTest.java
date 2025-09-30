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
import static com.palantir.javaformat.java.RemoveUnusedDeclarations.removeUnusedDeclarations;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import com.palantir.javaformat.jupiter.ParameterizedClass;
import java.util.List;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** {@link RemoveUnusedImports}Test */
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(ParameterizedClass.class)
public record RemoveUnusedDeclarationsTest(String input, String expected) {

    @ParameterizedClass.Parameters(name = "{index}: {0}")
    public static List<Object[]> parameters() {
        String[][][] inputsOutputs = {
            // Interface members
            {
                {
                    """
                    interface TestInterface {
                      public static final int CONSTANT = 1;
                      public abstract void method();
                      public static class InnerClass {}
                    }
                    """
                },
                {
                    """
                    interface TestInterface {
                      int CONSTANT = 1;
                      void method();
                      class InnerClass {}
                    }
                    """
                }
            },

            // Final parameters (should be preserved)
            {
                {
                    """
                    class Test {
                      void method(final String param1, @Nullable final String param2) {}
                    }
                    """
                },
                {
                    """
                    class Test {
                      void method(final String param1, @Nullable final String param2) {}
                    }
                    """
                }
            },

            // Code that shouldn't change
            {
                {
                    """
                    class NoChanges {
                      private int field;
                      void method(String param) {}
                      static final class Inner {}
                    }
                    """
                },
                {
                    """
                    class NoChanges {
                      private int field;
                      void method(String param) {}
                      static final class Inner {}
                    }
                    """
                }
            },

            // Annotation declarations
            //            {
            //                {
            //                    """
            //                    public @interface TestAnnotation {
            //                      public abstract String value();
            //                      public static final int DEFAULT = 0;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    public @interface TestAnnotation {
            //                      String value();
            //                      int DEFAULT = 0;
            //                    }
            //                    """
            //                }
            //            },

            // Nested interfaces and classes
            //            {
            //                {
            //                    """
            //                    class Outer {
            //                      public static interface InnerInterface {
            //                        public static final int VAL = 1;
            //                      }
            //                      public static class InnerClass {
            //                        public static final int VAL = 1;
            //                      }
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    class Outer {
            //                      interface InnerInterface {
            //                        int VAL = 1;
            //                      }
            //                      static class InnerClass {
            //                        static final int VAL = 1;
            //                      }
            //                    }
            //                    """
            //                }
            //            },

            // Static interfaces in abstract classes
            {
                {
                    """
                    public abstract class Test {
                      public static final int CONST1 = 1;
                      private static final int CONST2 = 2;
                      protected abstract void doSomething(final String param);
                      public static interface Inner {
                        public static final int INNER_CONST = 3;
                      }
                    }
                    """
                },
                {
                    """
                    public abstract class Test {
                      public static final int CONST1 = 1;
                      private static final int CONST2 = 2;
                      protected abstract void doSomething(final String param);
                      interface Inner {
                        int INNER_CONST = 3;
                      }
                    }
                    """
                }
            }
        };
        ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
        for (String[][] inputAndOutput : inputsOutputs) {
            assertThat(inputAndOutput.length).isEqualTo(2);
            builder.add(new String[] {
                Joiner.on('\n').join(inputAndOutput[0]) + '\n', Joiner.on('\n').join(inputAndOutput[1]) + '\n',
            });
        }
        return builder.build();
    }

    @TestTemplate
    public void removeUnused() throws FormatterException {
        Truth.assertThat(removeUnusedDeclarations(input)).isEqualTo(expected);
    }
}
