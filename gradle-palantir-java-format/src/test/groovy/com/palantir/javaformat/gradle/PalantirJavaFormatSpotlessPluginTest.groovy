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
import nebula.test.functional.GradleRunner
import nebula.test.functional.internal.classpath.ClasspathAddingInitScriptBuilder
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream

class PalantirJavaFormatSpotlessPluginTest extends IntegrationTestKitSpec {
    /** ./gradlew writeImplClasspath generates this file. */
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath
    private static final NATIVE_IMAGE_FILE = new File("build/nativeImage.path")
    private static final NATIVE_CONFIG = String.format("palantirJavaFormatNative files(\"%s\")", NATIVE_IMAGE_FILE.text)
    private static final String INIT_FILE_NAME = "init.gradle";


    @Unroll
    def "formats with spotless when spotless is applied"(String extraGradleProperties, String javaVersion, String expectedOutput) {
        File initScript = new File(projectDir, INIT_FILE_NAME)
        ClasspathAddingInitScriptBuilder.build(initScript, getBuildPluginClasspathInjector().toList())

        def extraDependencies = extraGradleProperties.isEmpty() ? "" : NATIVE_CONFIG
        settingsFile << """
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
        """.stripIndent(true)

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
            
        """.stripIndent()

        // Add jvm args to allow spotless and formatter gradle plugins to run with Java 16+
        file('gradle.properties') << """
        org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
        palantir.jdk.setup.enabled=true
        """.stripIndent()
        file('gradle.properties') << extraGradleProperties
        runTasks('wrapper')

        buildFile << """
            apply plugin: 'com.diffplug.spotless'
            
            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
                ${extraDependencies}
            }
        """.stripIndent()

        file('src/main/java/Main.java').text = invalidJavaFile


        when:
        def result = runGradlewTasks('spotlessApply', '--info')

        then:
        result.contains(expectedOutput)
        file('src/main/java/Main.java').text == validJavaFile

        where:
        extraGradleProperties               | javaVersion   | expectedOutput
        ""                                  | 21            | "Using the Java-based formatter"
        "palantir.native.formatter=true"    | 21            | "Using the Java-based formatter"
        "palantir.native.formatter=true"    | 17            | "Using the native-image formatter"

    }

    private static Iterable<File> getBuildPluginClasspathInjector() {
        return getPluginClasspathInjector(Path.of("../gradle-palantir-java-format/build/pluginUnderTestMetadata/plugin-under-test-metadata.properties"))
    }

    private static Iterable<File> getPluginClasspathInjector(Path path) {
        File propertiesFile = path.toFile()
        Properties properties = new Properties()
        propertiesFile.withInputStream { inputStream ->
            properties.load(inputStream)
        }
        String classpath = properties.getProperty('implementation-classpath')
        return classpath.split(File.pathSeparator).collect { new File(it) }
    }

    private String runGradlewTasks(String... tasks) {
        ProcessBuilder processBuilder = getProcessBuilder(tasks)
        Process process = processBuilder.start()
        String output = readAllInput(process.getInputStream())
        return output
    }

    public static String readAllInput(InputStream inputStream) {
        try (Stream<String> lines =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    private ProcessBuilder getProcessBuilder(String... tasks) {
        List<String> arguments = ["./gradlew", "--init-script", String.format("./%s", INIT_FILE_NAME)]
        Arrays.asList(tasks).forEach(arguments::add)
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(arguments)
                .directory(projectDir).redirectErrorStream(true)
        return processBuilder
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
