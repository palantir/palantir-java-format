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

class PalantirJavaFormatSpotlessPluginTest extends IntegrationTestKitSpec {
    /** ./gradlew writeImplClasspath generates this file. */
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath
    private static final NATIVE_IMAGE_FILE = new File("build/nativeImage.path")
    private static final NATIVE_CONFIG = String.format("palantirJavaFormatNative files(\"%s\")", NATIVE_IMAGE_FILE.text)


    @Unroll
    def "formats with spotless when spotless is applied"(String extraGradleProperties, String expectedOutput) {
        def extraDependencies = extraGradleProperties.isEmpty() ? "" : NATIVE_CONFIG
        buildFile << """
            // The 'com.diffplug.spotless:spotless-plugin-gradle' dependency is already added by palantir-java-format
            plugins {
                id 'java'
                id 'com.palantir.java-format'
            }
            
            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
                EXTRA_CONFIGURATION
            }
        """.replace("EXTRA_CONFIGURATION", extraDependencies).stripIndent()

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        file('gradle.properties') << """
        org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
        """.stripIndent()
        file('gradle.properties') << extraGradleProperties

        buildFile << """
            apply plugin: 'com.diffplug.spotless'
        """.stripIndent()

        file('src/main/java/Main.java').text = invalidJavaFile

        when:
        def result = runTasks('spotlessApply', '--info')

        then:
        result.output.contains(expectedOutput)
        file('src/main/java/Main.java').text == validJavaFile

        where:
        extraGradleProperties   | expectedOutput
        "" | "Using the legacy palantir-java-formatter"
        "palantir.native.formatter=true"  | "Using the native-image"

    }

    def validJavaFile = '''\
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
    '''.stripIndent()

    def invalidJavaFile = '''
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
    '''.stripIndent()
}
