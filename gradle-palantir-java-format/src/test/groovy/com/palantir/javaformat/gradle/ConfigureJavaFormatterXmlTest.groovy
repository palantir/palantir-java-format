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
            </root>
        """.stripIndent()

    public static final String EXPECTED = """\
        <root>
          <component name="PalantirJavaFormatSettings">
            <option name="implementationClassPath">
              <list>
                <option value="foo"/>
                <option value="bar"/>
              </list>
            </option>
          </component>
        </root>
        """.stripIndent()

    void testConfigure_missingEntireBlock_added() {
        def node = new XmlParser().parseText(MISSING_ENTIRE_BLOCK)

        when:
        ConfigureJavaFormatterXml.configure(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        xmlToString(node) == EXPECTED
    }

    void testConfigure_missingClassPath_added() {
        def node = new XmlParser().parseText(MISSING_CLASS_PATH)

        when:
        ConfigureJavaFormatterXml.configure(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        xmlToString(node) == """\
        <root>
          <component name="PalantirJavaFormatSettings">
            <option name="style" value="PALANTIR"/>
            <option name="implementationClassPath">
              <list>
                <option value="foo"/>
                <option value="bar"/>
              </list>
            </option>
          </component>
        </root>
        """.stripIndent()
    }

    void testConfigure_existingClassPath_modified() {
        def node = new XmlParser().parseText(EXISTING_CLASS_PATH)

        when:
        ConfigureJavaFormatterXml.configure(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        xmlToString(node) == EXPECTED
    }

    static String xmlToString(Node node) {
        StringWriter sw = new StringWriter();
        XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(sw));
        nodePrinter.setPreserveWhitespace(true);
        nodePrinter.print(node);
        return sw.toString()
    }
}
