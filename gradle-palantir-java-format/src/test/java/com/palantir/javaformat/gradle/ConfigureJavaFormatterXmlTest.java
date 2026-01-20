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

package com.palantir.javaformat.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import groovy.util.Node;
import groovy.xml.XmlNodePrinter;
import groovy.xml.XmlParser;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

class ConfigureJavaFormatterXmlTest {

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
                <option name="nativeImageClassPath" value="nativeFoo"/>
            </component>
        </root>
        """;

    private static final String MISSING_CLASS_PATH = """
        <root>
            <component name="PalantirJavaFormatSettings">
                <option name="style" value="PALANTIR"/>
            </component>
        </root>
        """;

    private static final String MISSING_ENTIRE_BLOCK = """
        <root>
        </root>
        """;

    private static final String EXPECTED = """
        <root>
          <component name="PalantirJavaFormatSettings">
            <option name="enabled" value="true"/>
            <option name="implementationClassPath">
              <list>
                <option value="foo"/>
                <option value="bar"/>
              </list>
            </option>
            <option name="nativeImageClassPath" value="nativeFoo"/>
          </component>
        </root>
        """;

    private static final String EXPECTED_WITHOUT_NATIVE = """
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
        """;

    static Stream<String> actionsOnSave() {
        return Stream.of("Format", "Optimize");
    }

    @Test
    void configure_missingEntireBlock_added() throws Exception {
        Node node = parseXml(MISSING_ENTIRE_BLOCK);

        ConfigureJavaFormatterXml.configureJavaFormat(
                node, List.of(URI.create("foo"), URI.create("bar")), Optional.of(URI.create("nativeFoo")));

        assertThat(xmlToString(node)).isEqualTo(EXPECTED);
    }

    @Test
    void configure_missingClassPath_added() throws Exception {
        Node node = parseXml(MISSING_CLASS_PATH);

        ConfigureJavaFormatterXml.configureJavaFormat(
                node, List.of(URI.create("foo"), URI.create("bar")), Optional.of(URI.create("nativeFoo")));

        String expected = """
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
                <option name="nativeImageClassPath" value="nativeFoo"/>
              </component>
            </root>
            """;

        assertThat(xmlToString(node)).isEqualTo(expected);
    }

    @Test
    void configure_existingClassPath_modified() throws Exception {
        Node node = parseXml(EXISTING_CLASS_PATH);

        ConfigureJavaFormatterXml.configureJavaFormat(
                node, List.of(URI.create("foo"), URI.create("bar")), Optional.of(URI.create("nativeFoo")));

        assertThat(xmlToString(node)).isEqualTo(EXPECTED);
    }

    @Test
    void configure_noNativeImageClassPath_removal() throws Exception {
        Node node = parseXml(EXISTING_CLASS_PATH);

        ConfigureJavaFormatterXml.configureJavaFormat(
                node, List.of(URI.create("foo"), URI.create("bar")), Optional.empty());

        assertThat(xmlToString(node)).isEqualTo(EXPECTED_WITHOUT_NATIVE);
    }

    @ParameterizedTest
    @MethodSource("actionsOnSave")
    void adds_action_OnSave_block_where_none_exists(String action) throws Exception {
        Node node = parseXml("""
            <root>
            </root>
            """);

        ConfigureJavaFormatterXml.configureWorkspaceXml(node);

        String newXml = xmlSubcomponentToString(node, action + "OnSaveOptions").strip();

        String expected = """
            <component name="%sOnSaveOptions">
              <option name="myRunOnSave" value="true"/>
              <option name="myAllFileTypesSelected" value="false"/>
              <option name="mySelectedFileTypes">
                <set>
                  <option value="JAVA"/>
                </set>
              </option>
            </component>
            """.formatted(action).strip();

        assertThat(newXml).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("actionsOnSave")
    void adds_Java_to_existing_action_OnSave_block(String action) throws Exception {
        Node node = parseXml("""
            <root>
              <component name="%sOnSaveOptions">
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="Go"/>
                  </set>
                </option>
              </component>
            </root>
            """.formatted(action));

        ConfigureJavaFormatterXml.configureWorkspaceXml(node);
        String newXml = xmlSubcomponentToString(node, action + "OnSaveOptions");

        String expected = """
            <component name="%sOnSaveOptions">
              <option name="mySelectedFileTypes">
                <set>
                  <option value="Go"/>
                  <option value="JAVA"/>
                </set>
              </option>
              <option name="myRunOnSave" value="true"/>
              <option name="myAllFileTypesSelected" value="false"/>
            </component>
            """.formatted(action).strip();

        assertThat(newXml).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("actionsOnSave")
    void if_all_file_types_are_already_configured_to_action_on_save_dont_change_anything(String action)
            throws Exception {
        Node node = parseXml("""
            <root>
              <component name="%sOnSaveOptions">
                <option name="myAllFileTypesSelected" value="true"/>
              </component>
            </root>
            """.formatted(action));

        ConfigureJavaFormatterXml.configureWorkspaceXml(node);
        String newXml = xmlSubcomponentToString(node, action + "OnSaveOptions").strip();

        String expected = """
            <component name="%sOnSaveOptions">
              <option name="myAllFileTypesSelected" value="true"/>
              <option name="myRunOnSave" value="true"/>
              <option name="mySelectedFileTypes">
                <set>
                  <option value="JAVA"/>
                </set>
              </option>
            </component>
            """.formatted(action).strip();

        assertThat(newXml).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("actionsOnSave")
    void if_the_myRunOnSave_for_action_on_save_is_explicitly_disabled_turn_it_on(String action) throws Exception {
        Node node = parseXml("""
            <root>
              <component name="%sOnSaveOptions">
                <option name="myRunOnSave" value="false"/>
                <option name="myAllFileTypesSelected" value="false"/>
                <option name="mySelectedFileTypes">
                  <set>
                    <option value="JAVA"/>
                  </set>
                </option>
              </component>
            </root>
            """.formatted(action));

        ConfigureJavaFormatterXml.configureWorkspaceXml(node);
        String newXml = xmlSubcomponentToString(node, action + "OnSaveOptions");

        String expected = """
            <component name="%sOnSaveOptions">
              <option name="myRunOnSave" value="true"/>
              <option name="myAllFileTypesSelected" value="false"/>
              <option name="mySelectedFileTypes">
                <set>
                  <option value="JAVA"/>
                </set>
              </option>
            </component>
            """.formatted(action).strip();

        assertThat(newXml).isEqualTo(expected);
    }

    private static Node parseXml(String xml) throws ParserConfigurationException, SAXException {
        try {
            return new XmlParser().parseText(xml);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String xmlSubcomponentToString(Node node, String name) {
        return ((List<Node>) node.children())
                .stream()
                        .filter(child -> name.equals(child.attribute("name")))
                        .findFirst()
                        .map(ConfigureJavaFormatterXmlTest::xmlToString)
                        .map(String::strip)
                        .orElseThrow(() -> new IllegalArgumentException("Component not found: " + name));
    }

    private static String xmlToString(Node node) {
        StringWriter sw = new StringWriter();
        XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(sw));
        nodePrinter.setPreserveWhitespace(true);
        nodePrinter.print(node);
        return sw.toString();
    }
}
