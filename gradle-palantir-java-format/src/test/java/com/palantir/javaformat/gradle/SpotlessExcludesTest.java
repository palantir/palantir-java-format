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
package com.palantir.javaformat.gradle;

import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@GradlePluginTests
class SpotlessExcludesTest {

    private static final String CLASSPATH_FILE = new File("build/impl.classpath").getAbsolutePath();

    @BeforeEach
    void setup(RootProject project) {
        project.buildGradle()
                .plugins()
                .add("java")
                .add("com.palantir.java-format")
                .add("com.diffplug.spotless");

        project.buildGradle().append("""
            dependencies {
                palantirJavaFormat files(file("%s").text.split(':'))
            }
            """, CLASSPATH_FILE);
        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        project.gradlePropertiesFile()
                .appendProperty(
                        "org.gradle.jvmargs",
                        "--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "build/generated/java",
                "src/generated/java",
                "src/main/generated_testsrc",
                "src/main/generated_foo_testsrc",
                "generated_testSrc",
                "build/groovy-dsl-plugins/output"
            })
    void format_ignores_excluded_directories(String srcDir, GradleInvoker gradle, RootProject project) {
        project.buildGradle().append("""
            sourceSets {
                main {
                    java { srcDir '%s' }
                }
            }
            """, srcDir);

        project.file(srcDir + "/test/Test.java").overwrite("""
            package test;
            import java.lang.Void;
            public class Test { Void test() { return null; } }
            """);

        InvocationResult result = gradle.withArgs("spotlessJavaCheck").buildsSuccessfully();

        assertThat(result).task(":spotlessJava").succeeded();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "foo/generated_foo/src/bar",
                "src/main/java/test/generatedNamespace",
            })
    void format_checks_non_generated_files(String srcDir, GradleInvoker gradle, RootProject project) {
        project.buildGradle().append("""
            sourceSets {
                main {
                    java { srcDir '%s' }
                }
            }
            """, srcDir);

        project.file(srcDir + "/Test.java").overwrite("""
            package test;
            import java.lang.Void;
            public class Test { Void test() { return null; } }
            """);

        InvocationResult result = gradle.withArgs("spotlessJavaCheck").buildsWithFailure();

        assertThat(result).task(":spotlessJava").succeeded();
    }
}
