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


class FormatterLazinessTest extends IntegrationTestKitSpec {
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath

    private GradlewExecutor executor

    def setup() {
        definePluginOutsideOfPluginBlock = true
        keepFiles = true
        executor = new GradlewExecutor(projectDir)
    }

    def "formatter is not loaded even if spotlessJava is realized lazily"() {
        // language=Gradle
        buildFile << """
        buildscript {
            repositories {
                maven {
                    url 'https://artifactory.palantir.build/artifactory/release-jar'
                    metadataSources { mavenPom(); ignoreGradleMetadataRedirection() }
                }
            }

            dependencies {
                classpath 'com.diffplug.spotless:spotless-plugin-gradle:6.22.0'
            }
        } 
        
        allprojects {
            apply plugin: 'java'
            apply plugin: 'com.diffplug.spotless'
            apply plugin: 'com.palantir.java-format'

            repositories {
                maven { url 'https://artifactory.palantir.build/artifactory/release-jar' }
            }
        }

        project.getTasks().getByName("spotlessJava")
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
        def result = executor.runGradlewTasks('--info')

        then:
        assert result.success
        println(result.standardOutput)
        result.standardOutput.contains('BUILD SUCCESSFUL')
    }
}