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

/**
 * When we were getting gradle-baseline to support the configuration cache, spotless had some poorly written tasks
 * which caused issues with the configuration cache.
 *
 * Bumping spotless to 6.22.0 fixed this, but revealed a new error — the {@code palantirJavaFormat} configuration was
 * being <a href="https://github.com/palantir/palantir-java-format/blob/b7b5995df3be690780939c0d0cb2ec49b99c68c8/gradle-palantir-java-format/src/main/java/com/palantir/javaformat/gradle/spotless/NativePalantirJavaFormatStep.java#L45"> resolved eagerly</a>.
 *
 * gradle-consistent-versions enforces against resolving configurations at configuration time, and throws an error.
 *
 * This test forces creation of the spotless steps, which will reveal any eager resolution of configurations.
 */
class SupportsSpotless622 extends IntegrationTestKitSpec {
    private static final CLASSPATH_FILE = new File("build/impl.classpath").absolutePath

    private GradlewExecutor executor

    def setup() {
        definePluginOutsideOfPluginBlock = true
        keepFiles = true
        executor = new GradlewExecutor(projectDir)
    }

    def "PalantirJavaFormatPlugin works with spotless 6.22.0"() {
        // language=Gradle
        buildFile << '''
            buildscript {
                repositories {
                    mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                    gradlePluginPortal() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                }
                 dependencies {
                     classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.34.0'
                     classpath 'com.diffplug.spotless:spotless-plugin-gradle:6.22.0'
                 }
            }
    
            apply plugin: 'java'
            apply plugin: 'com.palantir.java-format'
            apply plugin: 'com.palantir.consistent-versions'
            apply plugin: 'com.diffplug.spotless'
    
            version = '0.1.0'
        '''.stripIndent(true)


        file("versions.props")
        file("versions.lock")

        runTasks('wrapper')

        buildFile << """
            dependencies {
                palantirJavaFormat files(file("${CLASSPATH_FILE}").text.split(':'))
            }

            // This forces the realization of the spotlessJava task, creating the spotless steps. 
            // If any configurations are eagerly resolved in the spotless steps, 
            // consistent-versions should catch it and throw here. 
            project.getTasks().getByName("spotlessJava")
        """.stripIndent(true)

        when:
        def result = executor.runGradlewTasks('classes', '--info')

        then:
        assert result.success
    }
}
