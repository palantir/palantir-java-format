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
import nebula.test.functional.internal.classpath.ClasspathAddingInitScriptBuilder
import org.gradle.api.invocation.Gradle
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream

class ConfigurationCacheTest extends IntegrationTestKitSpec {
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath

    private GradlewExecutor executor

    def setup() {
        definePluginOutsideOfPluginBlock = true
        keepFiles = true
        executor = new GradlewExecutor(projectDir)
    }

    def "can run classes"() {
        // language=Gradle
        buildFile << """
         buildscript {
            repositories {
                mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                gradlePluginPortal() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
            }
             dependencies {
                 classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.34.0'

                 constraints {
                     classpath 'com.diffplug.spotless:6.22.0'
                 }
             }
         }

         apply plugin: 'java'
         apply plugin: 'com.palantir.java-format'
         apply plugin: 'com.palantir.consistent-versions'

        version = '0.1.0'
        """.stripIndent(true)


        file("versions.props")
        file("versions.lock")

        runTasks('wrapper')

        buildFile << """
            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
            }
        """.stripIndent()

        when:
        def result = executor.runGradlewTasks('classes', '--configuration-cache', '--info')

        then:
        assert result.success
        println(result.standardOutput)
        result.standardOutput.contains('BUILD SUCCESSFUL')
    }
}
