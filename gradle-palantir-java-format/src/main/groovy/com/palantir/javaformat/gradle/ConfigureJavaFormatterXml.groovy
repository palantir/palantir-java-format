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

    static void configureWorkspaceXml(Node rootNode) {
        configureFormatOnSave(rootNode)
        configureOptimizeOnSave(rootNode)
    }

    private static void configureFormatOnSave(Node rootNode) {
        configureOnSaveAction(matchOrCreateChild(rootNode, 'component', [name: 'FormatOnSaveOptions']))
    }

    private static void configureOptimizeOnSave(Node rootNode) {
        configureOnSaveAction(matchOrCreateChild(rootNode, 'component', [name: 'OptimizeOnSaveOptions']))
    }

    private static void configureOnSaveAction(Node onSaveOptions) {
        // In IntelliJ <2023.3.7, the myRunOnSave option is default false.
        // In IntelliJ >=2023.3.7, it is default true, and if it is set to true IntelliJ will remove it from the XML
        //     the next time the file is saved or the actions on save options window is saved.
        // So to cover all cases, we always write it out and ensure it is true
        def myRunOnSave = matchOrCreateChild(onSaveOptions, 'option', [name: 'myRunOnSave'])
        myRunOnSave.attributes().put('value', 'true')

        // In IntelliJ <2023.3.7, the myAllFileTypesSelected option is default true.
        // In IntelliJ >=2023.3.7, it is default false, and if it is set to false IntelliJ will remove it from the XML
        //     the next time the file is saved or the actions on save options window is saved.
        // People may have manually set this to true, so we respect that setting is so. However, if it doesn't exist,
        //     we make sure it is explicitly set false to work in all IntelliJ versions.
        matchOrCreateChild(onSaveOptions, 'option', [name: 'myAllFileTypesSelected'], [value: 'false'])

        // Ensure that is per-file type settings are enabled, we ensure JAVA is part of the list.
        def mySelectedFileTypes = matchOrCreateChild(onSaveOptions, 'option', [name: 'mySelectedFileTypes'])
        def set = matchOrCreateChild(mySelectedFileTypes, 'set')
        matchOrCreateChild(set, 'option', [value: 'JAVA'])
    }

    private static Node matchOrCreateChild(Node base, String name, Map attributes = [:], Map defaults = [:]) {
        matchChild(base, name, attributes).orElseGet {
            base.appendNode(name, attributes + defaults)
        }
    }

    private static Optional<Node> matchChild(Node base, String name, Map attributes = [:]) {
        def child = base[name].find { it.attributes().entrySet().containsAll(attributes.entrySet()) }

        return Optional.ofNullable(child)
    }
}
