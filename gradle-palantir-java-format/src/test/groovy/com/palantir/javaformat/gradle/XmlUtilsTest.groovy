/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

import groovy.util.Node
import spock.lang.Specification
import spock.lang.TempDir

class XmlUtilsTest extends Specification {

    @TempDir
    File tempDir

    void 'creates new XML file with default structure'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            def component = new Node(root, "component")
            component.attributes().put("name", "TestComponent")
            def option = new Node(component, "option")
            option.attributes().put("name", "enabled")
            option.attributes().put("value", "true")
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component name="TestComponent">
                <option name="enabled" value="true"/>
              </component>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }

    void 'updates existing XML file preserving content'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")
        xmlFile.text = '''\
            <?xml version="1.0" encoding="UTF-8"?>
            <project version="4">
              <component name="ExistingComponent">
                <option name="old" value="data"/>
              </component>
            </project>
            '''.stripIndent()

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            def component = root.children().find { it.@name == "ExistingComponent" }
            def newOption = new Node(component, "option")
            newOption.attributes().put("name", "new")
            newOption.attributes().put("value", "added")
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component name="ExistingComponent">
                <option name="old" value="data"/>
                <option name="new" value="added"/>
              </component>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }

    void 'preserves nested XML structure'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            def component = new Node(root, "component")
            component.attributes().put("name", "TestSettings")

            def option = new Node(component, "option")
            option.attributes().put("name", "classPath")

            def list = new Node(option, "list")
            def item1 = new Node(list, "option")
            item1.attributes().put("value", "path1")
            def item2 = new Node(list, "option")
            item2.attributes().put("value", "path2")
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component name="TestSettings">
                <option name="classPath">
                  <list>
                    <option value="path1"/>
                    <option value="path2"/>
                  </list>
                </option>
              </component>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }

    void 'handles multiple attributes correctly'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            def component = new Node(root, "component")
            component.attributes().put("name", "Settings")
            component.attributes().put("enabled", "true")
            component.attributes().put("version", "1.0")
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component enabled="true" name="Settings" version="1.0"/>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }

    void 'round-trip conversion preserves structure'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")
        xmlFile.text = '''\
            <?xml version="1.0" encoding="UTF-8"?>
            <project version="4">
              <component name="PalantirJavaFormatSettings">
                <option name="enabled" value="true"/>
                <option name="implementationClassPath">
                  <list>
                    <option value="file:/path/to/jar1.jar"/>
                    <option value="file:/path/to/jar2.jar"/>
                  </list>
                </option>
              </component>
            </project>
            '''.stripIndent()

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            // No changes, just round-trip
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component name="PalantirJavaFormatSettings">
                <option name="enabled" value="true"/>
                <option name="implementationClassPath">
                  <list>
                    <option value="file:/path/to/jar1.jar"/>
                    <option value="file:/path/to/jar2.jar"/>
                  </list>
                </option>
              </component>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }

    void 'modifies node attributes'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")
        xmlFile.text = '''\
            <?xml version="1.0" encoding="UTF-8"?>
            <project version="4">
              <component name="Settings">
                <option name="enabled" value="false"/>
              </component>
            </project>
            '''.stripIndent()

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            def component = root.children().find { it.@name == "Settings" }
            def option = component.children().find { it.@name == "enabled" }
            option.attributes().put("value", "true")
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component name="Settings">
                <option name="enabled" value="true"/>
              </component>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }

    void 'removes nodes'() {
        given:
        def xmlFile = new File(tempDir, "test.xml")
        xmlFile.text = '''\
            <?xml version="1.0" encoding="UTF-8"?>
            <project version="4">
              <component name="Settings">
                <option name="toRemove" value="data"/>
                <option name="toKeep" value="data"/>
              </component>
            </project>
            '''.stripIndent()

        when:
        XmlUtils.updateIdeaXmlFile(xmlFile) { Node root ->
            def component = root.children().find { it.@name == "Settings" }
            def toRemove = component.children().find { it.@name == "toRemove" }
            component.children().remove(toRemove)
        }

        then:
        xmlFile.text.replaceAll('\\s+', ' ').trim() == '''\
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <project version="4">
              <component name="Settings">
                <option name="toKeep" value="data"/>
              </component>
            </project>
            '''.stripIndent().replaceAll('\\s+', ' ').trim()
    }
}
