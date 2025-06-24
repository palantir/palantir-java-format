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

package com.palantir.javaformat.gradle

import nebula.test.IntegrationTestKitSpec
import spock.lang.Unroll

class PalantirJavaFormatPluginTest extends IntegrationTestKitSpec {

    /** ./gradlew writeImplClasspath generates this file. */
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath
    private static final NATIVE_IMAGE_FILE = new File("build/nativeImage.path").absolutePath
    private static final NATIVE_CONFIG = "palantirJavaFormatNative files(file(\"${NATIVE_IMAGE_FILE}\").text)"

    @Unroll
    def 'formatDiff updates only lines changed in git diff'(String extraGradleProperties, String expectedOutput) {
        file('gradle.properties') << extraGradleProperties
        def extraDependencies = extraGradleProperties.isEmpty() ? "" : NATIVE_CONFIG
        buildFile << """
            plugins {
                id 'java'
                id 'com.palantir.java-format'
            }
            
            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
                EXTRA_CONFIGURATION
            }
            apply plugin: 'idea'
        """.replace("EXTRA_CONFIGURATION", extraDependencies).stripIndent()

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        file('gradle.properties') << """
            org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
        """.stripIndent()

        "git init".execute(Collections.emptyList(), projectDir).waitFor()
        "git config user.name Foo".execute(Collections.emptyList(), projectDir).waitFor()
        "git config user.email foo@bar.com".execute(Collections.emptyList(), projectDir).waitFor()

        file('src/main/java/Main.java') << '''
        class Main {
            public static void crazyExistingFormatting  (  String... args) {

            }
        }
        '''.stripIndent()

        "git add .".execute(Collections.emptyList(), projectDir).waitFor()
        "git commit -m Commit".execute(Collections.emptyList(), projectDir).waitFor()

        file('src/main/java/Main.java').text = '''
        class Main {
            public static void crazyExistingFormatting  (  String... args) {
                                        System.out.println("Reformat me please");
                // some comments
                                                System.out.println("Reformat me again please");
            }
        }
        '''.stripIndent()

        when:
        def result = runTasks('formatDiff', '--info')

        then:
        result.output.contains(expectedOutput)
        file('src/main/java/Main.java').text == '''
        class Main {
            public static void crazyExistingFormatting  (  String... args) {
                System.out.println("Reformat me please");
                // some comments
                System.out.println("Reformat me again please");
            }
        }
        '''.stripIndent()

        where:
        extraGradleProperties | expectedOutput
        "" | "Using the Java-based formatter"
        "palantir.native.formatter=true" |  "Using the native-image formatter"
    }
}
