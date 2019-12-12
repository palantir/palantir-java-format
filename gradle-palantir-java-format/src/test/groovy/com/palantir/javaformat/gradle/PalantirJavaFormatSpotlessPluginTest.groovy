package com.palantir.javaformat.gradle

import nebula.test.IntegrationTestKitSpec

class PalantirJavaFormatSpotlessPluginTest extends IntegrationTestKitSpec {
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
        """.stripIndent()
    }

    def "formats with spotless when spotless is applied"() {
        buildFile << """
            apply plugin: 'com.diffplug.gradle.spotless'
        """.stripIndent()

        file('src/main/java/Main.java').text = invalidJavaFile

        when:
        runTasks('spotlessApply')

        then:
        file('src/main/java/Main.java').text == validJavaFile
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
