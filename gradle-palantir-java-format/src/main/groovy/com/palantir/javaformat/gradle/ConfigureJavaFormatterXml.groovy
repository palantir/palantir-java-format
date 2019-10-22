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

class ConfigureJavaFormatterXml {
    static void configureJavaFormat(Node rootNode, List<URI> uris) {
        def settings = matchOrCreateChild(rootNode, 'component', [name: 'PalantirJavaFormatSettings'])
        // enable
        matchOrCreateChild(settings, 'option', [name: 'enabled']).attributes().put('value', 'true')
        // configure classpath
        def classPath = matchOrCreateChild(settings, 'option', [name: 'implementationClassPath'])
        def listItems = matchOrCreateChild(classPath, 'list')
        listItems.children().clear()
        uris.forEach { URI uri ->
            listItems.appendNode('option', [value: uri])
        }
    }

    static void configureExternalDependencies(Node rootNode) {
        def externalDependencies = matchOrCreateChild(rootNode, 'component', [name: 'ExternalDependencies'])
        matchOrCreateChild(externalDependencies, 'plugin', [id: 'palantir-java-format'])
    }

    private static Node matchOrCreateChild(Node base, String name, Map attributes = [:], Map defaults = [:]) {
        def child = base[name].find { it.attributes().entrySet().containsAll(attributes.entrySet()) }
        if (child) {
            return child
        }

        return base.appendNode(name, attributes + defaults)
    }
}
