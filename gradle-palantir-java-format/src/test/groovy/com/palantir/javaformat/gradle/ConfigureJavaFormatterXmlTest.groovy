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
import spock.lang.Unroll

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
    public static final ArrayList<String> ACTIONS_ON_SAVE = ['Format', 'Optimize']

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

    @Unroll
    void 'adds #action OnSave block where none exists'(action) {
        // language=xml
        def node = new XmlParser().parseText '''
            <root>
            </root>
        '''.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureWorkspaceXml(node)

        def newXml = xmlSubcomponentToString(node, "${action}OnSaveOptions").strip()

        then:
        def expected = """
            <component name="${action}OnSaveOptions">
              <option name="mySelectedFileTypes">
                <set>
                  <option value="JAVA"/>
                </set>
              </option>
            </component>
        """.stripIndent(true).strip()

        newXml == expected

        where:
        action << ACTIONS_ON_SAVE
    }

    @Unroll
    void 'adds Java to existing #action OnSave block'(action) {
        def node = new XmlParser().parseText """
            <root>
              <component name="${action}OnSaveOptions">
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="Go"/>
                  </set>
                </option>
              </component>
            </root>
        """.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureWorkspaceXml(node)
        def newXml = xmlSubcomponentToString(node, "${action}OnSaveOptions")

        then:
        def expected = """
            <component name="${action}OnSaveOptions">
              <option name="mySelectedFileTypes">
                <set>
                  <option value="Go"/>
                  <option value="JAVA"/>
                </set>
              </option>
            </component>
        """.stripIndent(true).strip()

        newXml == expected

        where:
        action << ACTIONS_ON_SAVE
    }

    @Unroll
    void 'if all file types are already configured to #action on save, dont change anything'() {
        def node = new XmlParser().parseText """
            <root>
              <component name="${action}OnSaveOptions">
                <option name="myAllFileTypesSelected" value="true"/>
              </component>
            </root>
        """.stripIndent(true)

        when:
        ConfigureJavaFormatterXml.configureWorkspaceXml(node)
        def newXml = xmlSubcomponentToString(node, "${action}OnSaveOptions").strip()

        then:
        def expected = """
            <component name="${action}OnSaveOptions">
              <option name="myAllFileTypesSelected" value="true"/>
            </component>
        """.stripIndent(true).strip()

        newXml == expected

        where:
        action << ACTIONS_ON_SAVE
    }

    private static String xmlSubcomponentToString(Node node, String name) {
        xmlToString(node.children().find { it.@name == name }).strip()
    }

    static String xmlToString(Node node) {
        StringWriter sw = new StringWriter();
        XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(sw));
        nodePrinter.setPreserveWhitespace(true);
        nodePrinter.print(node);
        return sw.toString()
    }
}
