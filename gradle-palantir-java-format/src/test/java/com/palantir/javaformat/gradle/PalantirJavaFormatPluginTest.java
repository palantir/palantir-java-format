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
import com.palantir.gradle.testing.files.gradle.GradleFile;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@GradlePluginTests
class PalantirJavaFormatPluginTest {

    /** ./gradlew writeImplClasspath generates this file. */
    private static final String CLASSPATH_FILE = new File("build/impl.classpath").getAbsolutePath();

    private static final String NATIVE_IMAGE_FILE = new File("build/nativeImage.path").getAbsolutePath();

    private static final String NATIVE_CONFIG =
            "palantirJavaFormatNative files(file(\"" + NATIVE_IMAGE_FILE + "\").text)";

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                " | Using the Java-based formatter",
                "palantir.native.formatter=true | Using the native-image formatter"
            })
    void formatDiff_updates_only_lines_changed_in_git_diff(
            String extraGradleProperties, String expectedOutput, GradleInvoker gradle, RootProject project)
            throws IOException, InterruptedException {
        if (extraGradleProperties != null && !extraGradleProperties.isBlank()) {
            project.gradlePropertiesFile().append("%s\n", extraGradleProperties);
        }

        String extraDependencies =
                (extraGradleProperties == null || extraGradleProperties.isBlank()) ? "" : NATIVE_CONFIG;

        standardBuildFile(project, extraDependencies);

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        project.gradlePropertiesFile().append("""
            org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
            """);

        executeGitCommand(project, "git", "init");
        executeGitCommand(project, "git", "config", "user.name", "Foo");
        executeGitCommand(project, "git", "config", "user.email", "foo@bar.com");

        project.mainSourceSet().java().writeClass("""
            class Main {
                public static void crazyExistingFormatting  (  String... args) {

                }
            }
            """);

        executeGitCommand(project, "git", "add", ".");
        executeGitCommand(project, "git", "commit", "-m", "Commit");

        project.mainSourceSet().java().fileByClassName("Main").overwrite("""
            class Main {
                public static void crazyExistingFormatting  (  String... args) {
                                            System.out.println("Reformat me please");
                    // some comments
                                                    System.out.println("Reformat me again please");
                }
            }
            """);

        InvocationResult result = gradle.withArgs("formatDiff", "--info").buildsSuccessfully();

        assertThat(result).output().contains(expectedOutput);

        String expectedMainJava = """
            class Main {
                public static void crazyExistingFormatting  (  String... args) {
                    System.out.println("Reformat me please");
                    // some comments
                    System.out.println("Reformat me again please");
                }
            }
            """;

        project.mainSourceSet()
                .java()
                .fileByClassName("Main")
                .assertThat()
                .content()
                .isEqualTo(expectedMainJava);
    }

    private GradleFile standardBuildFile(RootProject project, String extraDependencies) {
        project.buildGradle()
                .plugins()
                .add("java")
                .add("com.palantir.java-format")
                .add("idea");

        project.buildGradle().append("""
            dependencies {
                palantirJavaFormat files(file("%s").text.split(':'))
                %s
            }
            """, CLASSPATH_FILE, extraDependencies);

        return project.buildGradle();
    }

    private void executeGitCommand(RootProject project, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(project.path().toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed with exit code " + exitCode);
        }
    }
}
