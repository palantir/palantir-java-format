/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

class FinlayTest extends IntegrationTestKitSpec {
    def setup() {
        // language=Gradle
        buildFile << """
         buildscript {
            repositories {
                mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                gradlePluginPortal() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                mavenLocal()
            }
             dependencies {
                 classpath 'com.palantir.baseline:gradle-baseline-java:999-CC7'
                 classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.34.0'
             }
         }
         
         apply plugin: 'com.palantir.baseline'
         apply plugin: 'com.palantir.consistent-versions'

        allprojects {
            apply plugin: 'com.palantir.java-format'
            apply plugin: 'java'
            
            repositories {
                mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
            }
        }
        version = '0.1.0'
        """.stripIndent(true)

        file("versions.props")
        file("versions.lock")

        file('gradle.properties') << """
        org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
          --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
        palantir.jdk.setup.enabled=true
        """.stripIndent(true)
        runTasks('wrapper')

        definePluginOutsideOfPluginBlock = true
        keepFiles = true
    }

    def "can run build"() {
        when:
        def result = runTasks('build')

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }
}
