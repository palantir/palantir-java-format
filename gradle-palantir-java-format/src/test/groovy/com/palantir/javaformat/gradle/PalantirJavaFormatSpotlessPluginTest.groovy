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

    private GradlewExecutor executor

    def setup() {
        executor = new GradlewExecutor(projectDir)
    }


    @Unroll
    def "formats with spotless when spotless is applied"(String extraGradleProperties, String javaVersion, String expectedOutput) {
        def extraDependencies = extraGradleProperties.isEmpty() ? "" : NATIVE_CONFIG
        settingsFile << '''
             buildscript {
                repositories {
                    mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                    gradlePluginPortal() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                }
                 dependencies {
                     classpath 'com.palantir.gradle.jdks:gradle-jdks-settings:0.62.0'
                 }
             }
            apply plugin: 'com.palantir.jdks.settings'
        '''.stripIndent(true)

        buildFile << """
             buildscript {
                repositories {
                    mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                    gradlePluginPortal() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                }
                 dependencies {
                     classpath 'com.palantir.baseline:gradle-baseline-java:6.21.0'
                     classpath 'com.palantir.gradle.jdks:gradle-jdks:0.62.0'
                     classpath 'com.palantir.gradle.jdkslatest:gradle-jdks-latest:0.17.0'

                     constraints {
                         classpath 'com.diffplug.spotless:6.22.0'
                     }
                 }
             }

            // The 'com.diffplug.spotless:spotless-plugin-gradle' dependency is already added by palantir-java-format
            plugins {
                id 'java'
            }

            apply plugin: 'com.palantir.java-format'     
            apply plugin: 'com.palantir.baseline-java-versions'
            apply plugin: 'com.palantir.jdks'
            apply plugin: 'com.palantir.jdks.latest'

            javaVersions {
                libraryTarget = ${javaVersion}
            }
            
            jdks {
                daemonTarget = ${javaVersion}
            }
        """.stripIndent(true)

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        file('gradle.properties') << '''
            org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
              --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
            palantir.jdk.setup.enabled=true
        '''.stripIndent(true)
        file('gradle.properties') << extraGradleProperties
        runTasks('wrapper')

        buildFile << """
            apply plugin: 'com.diffplug.spotless'

            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
                ${extraDependencies}
            }
        """.stripIndent(true)

        file('src/main/java/Main.java').text = invalidJavaFile

        when:
        def result = executor.runGradlewTasks('spotlessApply', '--info')
        def output = result.standardOutput
        def formattedFile = file('src/main/java/Main.java').text

        then:
        output.contains(expectedOutput)
        formattedFile == validJavaFile

        where:
        extraGradleProperties               | javaVersion   | expectedOutput
        ""                                  | 21            | "Using the Java-based formatter"
        "palantir.native.formatter=true"    | 21            | "Using the Java-based formatter"
        "palantir.native.formatter=true"    | 17            | "Using the native-image formatter"

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
    '''.stripIndent(true)

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
    '''.stripIndent(true)
}
