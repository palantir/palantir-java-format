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

import spock.lang.Specification

class ConfigureJavaFormatterXmlTest extends Specification {

    private static final String EXISTING_CLASS_PATH = """
            <root>
                <component name="PalantirJavaFormatSettings">
                    <option name="enabled" value="true"/>
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
            <option name="enabled" value="true"/>
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
        ConfigureJavaFormatterXml.configureJavaFormat(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        xmlToString(node) == EXPECTED
    }

    void testConfigure_missingClassPath_added() {
        def node = new XmlParser().parseText(MISSING_CLASS_PATH)

        when:
        ConfigureJavaFormatterXml.configureJavaFormat(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        xmlToString(node) == """\
        <root>
          <component name="PalantirJavaFormatSettings">
            <option name="style" value="PALANTIR"/>
            <option name="enabled" value="true"/>
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
        ConfigureJavaFormatterXml.configureJavaFormat(node, ['foo', 'bar'].collect { URI.create(it) })

        then:
        xmlToString(node) == EXPECTED
    }

    void 'adds FormatOnSave block where none exists'() {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureFormatOnSave(node)
        def newXml = xmlToString(node).strip()

        then:
        // language=xml
        def expected = '''
            <root>
              <component name="FormatOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="JAVA"/>
                  </set>
                </option>
              </component>
            </root>
        '''.stripIndent(true).strip()

        newXml == expected
    }

    void 'adds Java to existing FormatOnSave block'() {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
              <component name="FormatOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="Go"/>
                  </set>
                </option>
              </component>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureFormatOnSave(node)
        def newXml = xmlToString(node).strip()

        then:
        // language=xml
        def expected = '''
            <root>
              <component name="FormatOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="Go"/>
                    <option value="JAVA"/>
                  </set>
                </option>
              </component>
            </root>
        '''.stripIndent(true).strip()

        newXml == expected
    }

    void 'if all file types are already formatted on save, dont change anything'() {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
              <component name="FormatOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <!-- if myAllFileTypesSelected does not exist, it defaults to true -->
              </component>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureFormatOnSave(node)
        def newXml = xmlToString(node).strip()

        then:
        // language=xml
        def expected = '''
            <root>
              <component name="FormatOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
              </component>
            </root>
        '''.stripIndent(true).strip()

        newXml == expected
    }

    void 'adds OptimizeOnSave block where none exists'() {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureFormatOnSave(node)
        def newXml = xmlToString(node).strip()

        then:
        // language=xml
        def expected = '''
            <root>
              <component name="OptimizeOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="JAVA"/>
                  </set>
                </option>
              </component>
            </root>
        '''.stripIndent(true).strip()

        newXml == expected
    }

    void 'adds Java to existing OptimizeOnSave block'() {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
              <component name="OptimizeOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="Go"/>
                  </set>
                </option>
              </component>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureFormatOnSave(node)
        def newXml = xmlToString(node).strip()

        then:
        // language=xml
        def expected = '''
            <root>
              <component name="OptimizeOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="Go"/>
                    <option value="JAVA"/>
                  </set>
                </option>
              </component>
            </root>
        '''.stripIndent(true).strip()

        newXml == expected
    }

    void 'if all file types are already optimised on save, dont change anything'() {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
              <component name="FormatOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
                <!-- if myAllFileTypesSelected does not exist, it defaults to true -->
              </component>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureFormatOnSave(node)
        def newXml = xmlToString(node).strip()

        then:
        // language=xml
        def expected = '''
            <root>
              <component name="OptimizeOnSaveOptions">
                <option name="myRunOnSave" value="true"/>
              </component>
            </root>
        '''.stripIndent(true).strip()

        newXml == expected
    }

    static String xmlToString(Node node) {
        StringWriter sw = new StringWriter();
        XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(sw));
        nodePrinter.setPreserveWhitespace(true);
        nodePrinter.print(node);
        return sw.toString()
    }
}
