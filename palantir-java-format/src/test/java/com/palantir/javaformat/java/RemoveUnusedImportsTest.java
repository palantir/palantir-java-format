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
public class RemoveUnusedImportsTest {

    @ParameterizedClass.Parameters(name = "{index}: {0}")
    public static List<Object[]> parameters() {
        String[][][] inputsOutputs = {
            {
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;

                    class Test {
                      /** could be an {@link ArrayList} */
                      List<String> xs;
                    }
                    """
                },
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;

                    class Test {
                      /** could be an {@link ArrayList} */
                      List<String> xs;
                    }
                    """
                }
            },
            {
                {
                    """
                     package com.google.example;

                     import com.google.common.base.Preconditions;

                     import org.junit.runner.RunWith;
                     import org.junit.runners.JUnit4;

                     import java.util.List;
                     import java.util.Set;

                     import javax.annotations.Nullable;

                     import static org.junit.Assert.fail;
                     import static com.google.truth.Truth.assertThat;
                     import org.junit.jupiter.api.parallel.ExecutionMode; // unused

                     @RunWith( JUnit4.class ) public class SomeTest  {

                       <T> void check(@Nullable List<T> x) {
                         Preconditions.checkNodeNull(x);
                       }

                       void f() {
                         List<String> xs = null;
                         assertThat(xs).isNull();
                         try {
                           check(xs);
                           fail();
                         } catch (NullPointerException e) {
                         }
                       }
                     }
                     """
                },
                {
                    """
                     package com.google.example;

                     import com.google.common.base.Preconditions;

                     import org.junit.runner.RunWith;
                     import org.junit.runners.JUnit4;

                     import java.util.List;

                     import javax.annotations.Nullable;

                     import static org.junit.Assert.fail;
                     import static com.google.truth.Truth.assertThat;
                     // unused

                     @RunWith( JUnit4.class ) public class SomeTest  {

                       <T> void check(@Nullable List<T> x) {
                         Preconditions.checkNodeNull(x);
                       }

                       void f() {
                         List<String> xs = null;
                         assertThat(xs).isNull();
                         try {
                           check(xs);
                           fail();
                         } catch (NullPointerException e) {
                         }
                       }
                     }
                     """
                }
            },
            {
                {
                    """
                     package com.google.example;

                     import com.google.common.base.Preconditions;

                     import org.junit.runner.RunWith;
                     import org.junit.runners.JUnit4; // used with comment

                     import java.util.List; // new group java: empty line before
                     import java.util.Set;

                     import javax.annotations.Nullable; // new group javax: empty line before

                     import static org.junit.Assert.fail;
                     import static com.google.truth.Truth.assertThat;
                     import org.junit.jupiter.api.parallel.ExecutionMode; // unused

                     @RunWith( JUnit4.class ) public class SomeTest  {

                       <T> void check(@Nullable List<T> x) {
                         Preconditions.checkNodeNull(x);
                       }

                       void f() {
                         List<String> xs = null;
                         assertThat(xs).isNull();
                         try {
                           check(xs);
                           fail();
                         } catch (NullPointerException e) {
                         }
                       }
                     }
                     """
                },
                {
                    """
                     package com.google.example;

                     import com.google.common.base.Preconditions;

                     import org.junit.runner.RunWith;
                     import org.junit.runners.JUnit4; // used with comment

                     import java.util.List; // new group java: empty line before

                     import javax.annotations.Nullable; // new group javax: empty line before

                     import static org.junit.Assert.fail;
                     import static com.google.truth.Truth.assertThat;
                     // unused

                     @RunWith( JUnit4.class ) public class SomeTest  {

                       <T> void check(@Nullable List<T> x) {
                         Preconditions.checkNodeNull(x);
                       }

                       void f() {
                         List<String> xs = null;
                         assertThat(xs).isNull();
                         try {
                           check(xs);
                           fail();
                         } catch (NullPointerException e) {
                         }
                       }
                     }
                     """
                }
            },
            {
                {
                    """
                     package com.google.example;

                     import com.google.common.base.Preconditions;

                     import org.junit.runner.RunWith;
                     import org.junit.runners.JUnit4; // used with comment

                     import java.util.List;
                     import java.util.Set;
                     import javax.annotations.Nullable; // no empty line before

                     import static org.junit.Assert.fail;
                     import static com.google.truth.Truth.assertThat;
                     import org.junit.jupiter.api.parallel.ExecutionMode; // unused

                     @RunWith( JUnit4.class ) public class SomeTest  {

                       <T> void check(@Nullable List<T> x) {
                         Preconditions.checkNodeNull(x);
                       }

                       void f() {
                         List<String> xs = null;
                         assertThat(xs).isNull();
                         try {
                           check(xs);
                           fail();
                         } catch (NullPointerException e) {
                         }
                       }
                     }
                     """
                },
                {
                    """
                     package com.google.example;

                     import com.google.common.base.Preconditions;

                     import org.junit.runner.RunWith;
                     import org.junit.runners.JUnit4; // used with comment

                     import java.util.List;
                     import javax.annotations.Nullable; // no empty line before

                     import static org.junit.Assert.fail;
                     import static com.google.truth.Truth.assertThat;
                     // unused

                     @RunWith( JUnit4.class ) public class SomeTest  {

                       <T> void check(@Nullable List<T> x) {
                         Preconditions.checkNodeNull(x);
                       }

                       void f() {
                         List<String> xs = null;
                         assertThat(xs).isNull();
                         try {
                           check(xs);
                           fail();
                         } catch (NullPointerException e) {
                         }
                       }
                     }
                     """
                }
            },
            {
                {
                    """
                    import java.util.ArrayList;
                    import java.util.Collection;
                    /** {@link ArrayList#add} {@link Collection#remove(Object)} */
                    class Test {}
                    """
                },
                {
                    """
                    import java.util.ArrayList;
                    import java.util.Collection;
                    /** {@link ArrayList#add} {@link Collection#remove(Object)} */
                    class Test {}
                    """
                }
            },
            {
                {
                    """
                    import a.A;
                    import a.B;
                    import a.C;
                    class Test {
                      /** a
                       * {@link A} */
                      void f() {}
                    }
                    """
                },
                {
                    """
                    import a.A;
                    class Test {
                      /** a
                       * {@link A} */
                      void f() {}
                    }
                    """
                }
            },
            {
                {
                    """
                    import a.A;import a.B;
                    import a.C; // hello
                    class Test {
                      B b;
                    }
                    """
                },
                {
                    """
                    import a.B;
                    // hello
                    class Test {
                      B b;
                    }
                    """
                }
            },
            {
                {
                    """
                    import a.A;
                    import b.B;
                    import c.C;
                    import d.D;
                    import e.E;
                    import f.F;
                    import g.G;
                    import h.H;
                    /**
                     * {@link A} {@linkplain B} {@value D#FOO}
                     *
                     * @exception E
                     * @throws F
                     * @see C
                     * @see H#foo
                     * @see <a href="whatever">
                     */
                    class Test {
                    }
                    """
                },
                {
                    """
                    import a.A;
                    import b.B;
                    import c.C;
                    import d.D;
                    import e.E;
                    import f.F;
                    import h.H;
                    /**
                     * {@link A} {@linkplain B} {@value D#FOO}
                     *
                     * @exception E
                     * @throws F
                     * @see C
                     * @see H#foo
                     * @see <a href="whatever">
                     */
                    class Test {
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.Map;
                    /** {@link Map.Entry#containsKey(Object)} } */
                    class Test {}
                    """
                },
                {
                    """
                    import java.util.Map;
                    /** {@link Map.Entry#containsKey(Object)} } */
                    class Test {}
                    """
                }
            },
            {
                {
                    """
                    /** {@link #containsKey(Object)} } */
                    class Test {}
                    """
                },
                {
                    """
                    /** {@link #containsKey(Object)} } */
                    class Test {}
                    """
                }
            },
            {
                {
                    """
                    import java.util.*;
                    class Test {
                      List<String> xs;
                    }
                    """
                },
                {
                    """
                    import java.util.*;
                    class Test {
                      List<String> xs;
                    }
                    """
                }
            },
            {
                {
                    """
                    package com.foo;
                    import static com.foo.Outer.A;
                    import com.foo.*;
                    import com.foo.B;
                    import com.bar.C;
                    class Test {
                      A a;
                      B b;
                      C c;
                    }
                    """
                },
                {
                    """
                    package com.foo;
                    import static com.foo.Outer.A;
                    import com.foo.B;
                    import com.bar.C;
                    class Test {
                      A a;
                      B b;
                      C c;
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.Map;
                    import java.util.Map.Entry;
                    /** {@link #foo(Map.Entry[])} */
                    public class Test {}
                    """
                },
                {
                    """
                    import java.util.Map;
                    /** {@link #foo(Map.Entry[])} */
                    public class Test {}
                    """
                }
            },
            {
                {
                    """
                    import java.util.List;
                    import java.util.Collection;
                    /** {@link java.util.List#containsAll(Collection)} */
                    public class Test {}
                    """
                },
                {
                    """
                    import java.util.Collection;
                    /** {@link java.util.List#containsAll(Collection)} */
                    public class Test {}
                    """
                }
            },
            //            {
            //                {
            //                    """
            //                    package p;
            //                    import java.lang.Foo;
            //                    import java.lang2.Foo;
            //                    import java.lang.Foo.Bar;
            //                    import p.Baz;
            //                    import p.Baz.Bork;
            //                    public class Test implements java.lang.Foo, Bar, Baz, Bork {}
            //                    """
            //                },
            //                {
            //                    """
            //                    package p;
            //                    import java.lang.Foo;
            //                    import java.lang.Foo.Bar;
            //                    import p.Baz;
            //                    import p.Baz.Bork;
            //                    public class Test implements Foo, Bar, Baz, Bork {}
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import java.lang.Foo;
                    interface Test { private static void foo() {} }
                    """
                },
                {"""
                    interface Test { private static void foo() {} }
                    """}
            },
            //            {
            //                {
            //                    """
            //                    package test.pkg;
            //
            //                    import static test.pkg.Constants.FOO;
            //                    import static test.pkg.Constants2.FOO;
            //                    import static test.pkg.Constants.BAR;
            //
            //                    import java.util.List;
            //                    import java.util.ArrayList;
            //
            //                    public class Test {
            //                        public static final String VALUE = Constants.FOO;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    package test.pkg;
            //
            //                    import static test.pkg.Constants.FOO;
            //
            //                    public class Test {
            //                        public static final String VALUE = Constants.FOO;
            //                    }
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import java.util.List;
                    import java.util.Collections;

                    class Test {
                      void foo() {
                        List<String> list = Collections.emptyList();
                      }
                    }
                    """
                },
                {
                    """
                    import java.util.List;
                    import java.util.Collections;

                    class Test {
                      void foo() {
                        List<String> list = Collections.emptyList();
                      }
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;
                    import java.util.Collections;

                    class Test {
                      List<String> list = new ArrayList<>();
                    }
                    """
                },
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;

                    class Test {
                      List<String> list = new ArrayList<>();
                    }
                    """
                }
            },
            {
                {
                    """
                    import static java.util.Collections.*;
                    import static java.util.Collections.emptyList;

                    class Test {
                      void foo() {
                        emptyList();
                      }
                    }
                    """
                },
                {
                    """
                    import static java.util.Collections.emptyList;

                    class Test {
                      void foo() {
                        emptyList();
                      }
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.List;
                    import java.util.function.Function;

                    class Test {
                      Function<List<String>, String> f;
                    }
                    """
                },
                {
                    """
                    import java.util.List;
                    import java.util.function.Function;

                    class Test {
                      Function<List<String>, String> f;
                    }
                    """
                }
            },
            {
                {
                    """
                    import a.Outer.Inner;
                    import a.Outer;

                    class Test {
                      Inner i;
                    }
                    """
                },
                {
                    """
                    import a.Outer.Inner;

                    class Test {
                      Inner i;
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.List;
                    import java.lang.Deprecated;

                    @Deprecated
                    class Test {}
                    """
                },
                {"""

                    @Deprecated
                    class Test {}
                    """}
            },
            {
                {
                    """
                    import java.util.HashMap;

                    class Test {
                      java.util.Map<String, String> map = new java.util.HashMap<>();
                    }
                    """
                },
                {
                    """

                    class Test {
                      java.util.Map<String, String> map = new java.util.HashMap<>();
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.Map;

                    class Test {
                      Map.Entry<String, String> entry;
                    }
                    """
                },
                {
                    """
                    import java.util.Map;

                    class Test {
                      Map.Entry<String, String> entry;
                    }
                    """
                }
            },
            {
                {
                    """
                    import static java.lang.Math.*;
                    import static java.lang.Math.PI;

                    class Test {
                      double r = PI;
                    }
                    """
                },
                {
                    """
                    import static java.lang.Math.PI;

                    class Test {
                      double r = PI;
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.ArrayList;

                    // This is a comment mentioning ArrayList
                    class Test {}
                    """
                },
                {
                    """

                    // This is a comment mentioning ArrayList
                    class Test {}
                    """
                }
            },
            {
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;

                    // Preserve this comment
                    class Test {
                      List<String> list;
                    }
                    """
                },
                {
                    """
                    import java.util.List;

                    // Preserve this comment
                    class Test {
                      List<String> list;
                    }
                    """
                }
            },
            //            {
            //                {
            //                    """
            //                    import pkg1.A;
            //                    import pkg2.A;
            //
            //                    class Test {
            //                      pkg1.A a1;
            //                      pkg2.A a2;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    import pkg1.A;
            //                    import pkg2.A;
            //
            //                    class Test {
            //                      pkg1.A a1;
            //                      pkg2.A a2;
            //                    }
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import java.lang.annotation.*;

                    @Retention(RetentionPolicy.RUNTIME)
                    @Target(ElementType.TYPE)
                    public @interface Test {}
                    """
                },
                {
                    """
                    import java.lang.annotation.*;

                    @Retention(RetentionPolicy.RUNTIME)
                    @Target(ElementType.TYPE)
                    public @interface Test {}
                    """
                }
            },
            //            {
            //                {
            //                    """
            //                    import static pkg.Constants.*;
            //                    import static pkg.Constants.VALUE;
            //
            //                    class Test {
            //                      String s = VALUE;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    import static pkg.Constants.*;
            //                    import static pkg.Constants.VALUE;
            //
            //                    class Test {
            //                      String s = VALUE;
            //                    }
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;

                    /**
                     * @see ArrayList
                     */
                    class Test {
                      List<String> list;
                    }
                    """
                },
                {
                    """
                    import java.util.List;
                    import java.util.ArrayList;

                    /**
                     * @see ArrayList
                     */
                    class Test {
                      List<String> list;
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.Map;
                    import java.util.HashMap;

                    class Test {
                      Map<String, String> map = new HashMap<>() {
                        {
                          put("key", "value");
                        }
                      };
                    }
                    """
                },
                {
                    """
                    import java.util.Map;
                    import java.util.HashMap;

                    class Test {
                      Map<String, String> map = new HashMap<>() {
                        {
                          put("key", "value");
                        }
                      };
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.concurrent.*;

                    class Test {
                      Future<?> future;
                    }
                    """
                },
                {
                    """
                    import java.util.concurrent.*;

                    class Test {
                      Future<?> future;
                    }
                    """
                }
            },
            {
                {
                    """
                    import javax.annotation.*;

                    class Test {
                      @Nullable String s;
                    }
                    """
                },
                {
                    """
                    import javax.annotation.*;

                    class Test {
                      @Nullable String s;
                    }
                    """
                }
            },
            //            {
            //                {
            //                    """
            //                    import java.util.*;
            //                    import java.util.stream.*;
            //
            //                    class Test {
            //                      Stream<String> stream;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    import java.util.stream.*;
            //
            //                    class Test {
            //                      Stream<String> stream;
            //                    }
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import java.time.*;

                    class Test {
                      LocalDate date;
                    }
                    """
                },
                {
                    """
                    import java.time.*;

                    class Test {
                      LocalDate date;
                    }
                    """
                }
            },
            {
                {
                    """
                    import pkg.Enclosing.*;

                    class Test {
                      Nested nested;
                    }
                    """
                },
                {
                    """
                    import pkg.Enclosing.*;

                    class Test {
                      Nested nested;
                    }
                    """
                }
            },
            {
                {
                    """
                    import pkg.Outer.Inner;

                    class Test {
                      Inner inner;
                    }
                    """
                },
                {
                    """
                    import pkg.Outer.Inner;

                    class Test {
                      Inner inner;
                    }
                    """
                }
            },
            {
                {
                    """
                    import java.util.function.*;

                    class Test {
                      Function<String, Integer> f;
                    }
                    """
                },
                {
                    """
                    import java.util.function.*;

                    class Test {
                      Function<String, Integer> f;
                    }
                    """
                }
            },
            //            {
            //                {
            //                    """
            //                    import static pkg.Constants.*;
            //
            //                    class Test {
            //                      String s = CONSTANT;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    import static pkg.Constants.*;
            //
            //                    class Test {
            //                      String s = CONSTANT;
            //                    }
            //                    """
            //                }
            //            },
            //            {
            //                {
            //                    """
            //                    import pkg.WithVeryLongName;
            //                    import pkg.WithVeryLongName.*;
            //
            //                    class Test {
            //                      WithVeryLongName w;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    import pkg.WithVeryLongName;
            //
            //                    class Test {
            //                      WithVeryLongName w;
            //                    }
            //                    """
            //                }
            //            },
            //            {
            //                {
            //                    """
            //                    import pkg.Generic;
            //                    import pkg.Generic.*;
            //
            //                    class Test {
            //                      Generic<String> g;
            //                    }
            //                    """
            //                },
            //                {
            //                    """
            //                    import pkg.Generic;
            //
            //                    class Test {
            //                      Generic<String> g;
            //                    }
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import java.util.*;
                    import java.util.concurrent.*;

                    class Test {
                      List<Future<String>> list;
                    }
                    """
                },
                {
                    """
                    import java.util.*;
                    import java.util.concurrent.*;

                    class Test {
                      List<Future<String>> list;
                    }
                    """
                }
            },
            {
                {
                    """
                    import pkg.Annotation;
                    import pkg.Other;

                    @Annotation
                    class Test {
                      Other o;
                    }
                    """
                },
                {
                    """
                    import pkg.Annotation;
                    import pkg.Other;

                    @Annotation
                    class Test {
                      Other o;
                    }
                    """
                }
            },
            {
                {
                    """
                    import pkg.Record;

                    record Test(Record r) {}
                    """
                },
                {
                    """
                    import pkg.Record;

                    record Test(Record r) {}
                    """
                }
            },
            //            {
            //                {
            //                    """
            //                    import pkg.Sealed;
            //                    import pkg.Permitted;
            //
            //                    sealed class Test permits Permitted {}
            //                    """
            //                },
            //                {
            //                    """
            //                    import pkg.Sealed;
            //                    import pkg.Permitted;
            //
            //                    sealed class Test permits Permitted {}
            //                    """
            //                }
            //            },
            {
                {
                    """
                    import pkg.WithTypeParameter<T>;

                    class Test<T> {
                      WithTypeParameter<T> w;
                    }
                    """
                },
                {
                    """
                    import pkg.WithTypeParameter<T>;

                    class Test<T> {
                      WithTypeParameter<T> w;
                    }
                    """
                }
            },
            {
                {
                    """
                    import pkg.WithWildcard<?>;

                    class Test {
                      WithWildcard<?> w;
                    }
                    """
                },
                {
                    """
                    import pkg.WithWildcard<?>;

                    class Test {
                      WithWildcard<?> w;
                    }
                    """
                }
            },
            {
                {
                    """
                    import pkg.WithBounds<T extends Number>;

                    class Test<T extends Number> {
                      WithBounds<T> w;
                    }
                    """
                },
                {
                    """
                    import pkg.WithBounds<T extends Number>;

                    class Test<T extends Number> {
                      WithBounds<T> w;
                    }
                    """
                }
            }
        };

        ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
        for (String[][] inputAndOutput : inputsOutputs) {
            assertThat(inputAndOutput).hasLength(2);
            builder.add(new String[] {
                Joiner.on('\n').join(inputAndOutput[0]) + '\n', Joiner.on('\n').join(inputAndOutput[1]) + '\n',
            });
        }
        return builder.build();
    }

    private final String input;
    private final String expected;

    public RemoveUnusedImportsTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @TestTemplate
    public void removeUnused() {
        Truth.assertThat(RemoveUnusedImports.removeUnusedImports(input)).isEqualTo(expected);
    }
}
