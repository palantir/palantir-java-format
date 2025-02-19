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

class PalantirJavaFormatIdeaPluginTest extends IntegrationTestKitSpec {

    private static final NATIVE_IMAGE_FILE = new File("build/nativeImage.path")
    private static final NATIVE_CONFIG = String.format("palantirJavaFormatNative files(\"%s\")", NATIVE_IMAGE_FILE.text)

    def "idea_configuresIpr"() {
        file('gradle.properties') << extraGradleProperties

        buildFile << """
            plugins {
                id 'com.palantir.java-format-idea'
            }
            apply plugin: 'idea'
            
            dependencies {
                palantirJavaFormat project.files() // no need to store the real thing in here
                EXTRA_CONFIGURATION
            }
        """.replace("EXTRA_CONFIGURATION", extraDependencies).stripIndent()

        when:
        runTasks('idea')

        then:
        def iprFile = new File(projectDir, "${moduleName}.ipr")
        def ipr = new XmlSlurper().parse(iprFile)
        def settings = ipr.component.findAll { it.@name == "PalantirJavaFormatSettings" }
        !settings.isEmpty()

        where:
        extraGradleProperties | extraDependencies
        "" | ""
        "palantir.native.formatter=true" | NATIVE_CONFIG
    }
}
