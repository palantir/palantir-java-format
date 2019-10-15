package com.palantir.javaformat.gradle


import nebula.test.IntegrationTestKitSpec

class JavaFormatPluginTest extends IntegrationTestKitSpec {
    private static def PLUGIN_ID = "com.palantir.java-format"

    void setup() {
        // Note: this deprecation is due to gradle-idea-ext-plugin 0.5, however they fixed the issue in master, which
        // is yet unreleased.
        // https://github.com/JetBrains/gradle-idea-ext-plugin/commit/348fabfa5f5471c3c555b2a76b3c38b84d075aad
        System.properties.'ignoreDeprecations' = 'true'

        buildFile << """
            plugins {
                id "${PLUGIN_ID}"
            }
            apply plugin: 'idea'
            
            dependencies {
                palantirJavaFormat project.files() // no need to store the real thing in here
            }
        """.stripIndent()
    }

    def "idea_configuresIpr"() {
        buildFile << """
        """.stripIndent()

        when:
        runTasks('idea')

        then:
        def iprFile = new File(projectDir, "${moduleName}.ipr")
        def ipr = new XmlSlurper().parse(iprFile)
        def settings = ipr.component.findAll { it.@name == "PalantirJavaFormatSettings" }
        !settings.isEmpty()
    }
}
