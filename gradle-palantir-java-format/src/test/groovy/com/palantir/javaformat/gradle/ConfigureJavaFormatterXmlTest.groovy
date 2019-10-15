package com.palantir.javaformat.gradle


import spock.lang.Specification

class ConfigureJavaFormatterXmlTest extends Specification {

    private static final String EXISTING_CLASS_PATH = """
            <root>
                <component name="PalantirJavaFormatSettings">
                    <option name="implementationClassPath">
                      <list>
                        <option value="foo" />
                        <option value="aldfjh://barz" />
                      </list>
                    </option>
                </component>
            </root>
        """.stripIndent()

    private static final String MISSING_CLASS_PATH = """
            <root>
                <component name="PalantirJavaFormatSettings">
                    <option name="style" value="PALANTIR"/>
                </component>
            </root>
        """.stripIndent()

    private static final String MISSING_ENTIRE_BLOCK = """
            <root>
                <component name="PalantirJavaFormatSettings">
                    <option name="style" value="PALANTIR"/>
                </component>
            </root>
        """.stripIndent()

    void testConfigure_missingEntireBlock_added() {
        def node = new XmlParser().parseText(MISSING_ENTIRE_BLOCK)

        when:
        ConfigureJavaFormatterXml.configure(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        def values = node
                .component.find { it.@name == 'PalantirJavaFormatSettings' }
                .option.find { it.@name == 'implementationClassPath' }
                .list
                .option
                .@value
        values.collect { it.toString() } == ['foo', 'bar']
    }

    void testConfigure_missingClassPath_added() {
        def node = new XmlParser().parseText(MISSING_CLASS_PATH)

        when:
        ConfigureJavaFormatterXml.configure(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        def values = node
                .component.find { it.@name == 'PalantirJavaFormatSettings' }
                .option.find { it.@name == 'implementationClassPath' }
                .list
                .option
                .@value
        values.collect { it.toString() } == ['foo', 'bar']
    }

    void testConfigure_existingClassPath_modified() {
        def node = new XmlParser().parseText(EXISTING_CLASS_PATH)

        when:
        ConfigureJavaFormatterXml.configure(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        def values = node
                .component.find { it.@name == 'PalantirJavaFormatSettings' }
                .option.find { it.@name == 'implementationClassPath' }
                .list
                .option
                .@value
        values.collect { it.toString() } == ['foo', 'bar']
    }
}
