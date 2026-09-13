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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.io.File;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@GradlePluginTests
class PalantirJavaFormatSpotlessPluginTest {

    /** ./gradlew writeImplClasspath generates this file. */
    private static final String CLASSPATH_FILE = new File("build/impl.classpath").getAbsolutePath();

    private static final String NATIVE_IMAGE_FILE = new File("build/nativeImage.path").getAbsolutePath();

    private static final String NATIVE_CONFIG =
            "palantirJavaFormatNative files(file(\"" + NATIVE_IMAGE_FILE + "\").text)";

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "                               | 21 | Using the Java-based formatter",
                "palantir.native.formatter=true | 21 | Using the Java-based formatter",
                "palantir.native.formatter=true | 17 | Using the native-image formatter"
            })
    void formats_with_spotless_when_spotless_is_applied(
            String extraGradleProperties,
            String javaVersion,
            String expectedOutput,
            GradleInvoker gradle,
            RootProject project) {

        String extraDependencies = Optional.ofNullable(extraGradleProperties)
                .map(props -> NATIVE_CONFIG)
                .orElse("");

        project.settingsGradle().plugins().add("com.palantir.jdks.settings");

        // The 'com.diffplug.spotless:spotless-plugin-gradle' dependency is already added by palantir-java-format
        project.buildGradle()
                .plugins()
                .add("java")
                .add("com.palantir.java-format")
                .add("com.palantir.baseline-java-versions")
                .add("com.palantir.jdks")
                .add("com.palantir.jdks.latest");

        project.buildGradle().append("""
            javaVersions {
                libraryTarget = %s
            }

            jdks {
                daemonTarget = %s
            }
            """, javaVersion, javaVersion);

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        project.gradlePropertiesFile()
                .appendProperty(
                        "org.gradle.jvmargs",
                        "--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED "
                                + "--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
                .appendProperty("palantir.jdk.setup.enabled", "true");

        project.gradlePropertiesFile()
                .appendLine(Optional.ofNullable(extraGradleProperties).orElse(""));

        gradle.withArgs("wrapper").buildsSuccessfully();

        project.buildGradle().plugins().add("com.diffplug.spotless");

        project.buildGradle().append("""
            dependencies {
                palantirJavaFormat files(file("%s").text.split(':'))
                %s
            }
            """, CLASSPATH_FILE, extraDependencies);

        project.file("src/main/java/Main.java").overwrite(invalidJavaFile());

        InvocationResult result = gradle.withArgs("spotlessApply", "--info").buildsSuccessfully();

        project.file("src/main/java/Main.java").assertThat().hasContent(validJavaFile());
        result.assertThat().output().contains(expectedOutput);
    }

    private String validJavaFile() {
        return """
            package test;

            public class Test {
                void test() {
                    int x = 1;
                    System.out.println("Hello");
                    Optional.of("hello").orElseGet(() -> {
                        return "Hello World";
                    });
                }
            }
            """;
    }

    private String invalidJavaFile() {
        return """
            package test;
            import com.java.unused;
            public class Test { void test() {int x = 1;
                System.out.println(
                    "Hello"
                );
                Optional.of("hello").orElseGet(() -> {
                    return "Hello World";
                });
            } }
            """;
    }
}
