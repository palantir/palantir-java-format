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

class PalantirJavaFormatPluginTest extends IntegrationTestKitSpec {

    /** ./gradlew writeImplClasspath generates this file. */
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath

    void setup() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.palantir.java-format'
            }
            
            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
            }
            apply plugin: 'idea'
        """.stripIndent()

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        file('gradle.properties') << """
        org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
        """.stripIndent()
    }

    def 'formatDiff updates only lines changed in git diff'() {
        when:
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
            }
        }
        '''.stripIndent()

        then:
        runTasks('formatDiff')
        file('src/main/java/Main.java').text == '''
        class Main {
            public static void crazyExistingFormatting  (  String... args) {
                System.out.println("Reformat me please");
            }
        }
        '''.stripIndent()
    }
}
