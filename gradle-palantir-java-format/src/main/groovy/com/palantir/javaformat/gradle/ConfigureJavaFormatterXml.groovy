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
        // If myRunOnSave is set to true, IntelliJ removes it. If it's set to false, we still need it to run.
        // So we should just remove it so we do run on save.
        matchChild(onSaveOptions, 'option', [name: 'myRunOnSave']).ifPresent { myRunOnSave ->
            onSaveOptions.remove(myRunOnSave)
        }

        def myAllFileTypesSelectedAlreadySet = matchChild(onSaveOptions, 'option', [name: 'myAllFileTypesSelected'])
                .map { Boolean.parseBoolean(it.attribute('value')) }
                // If myAllFileTypesSelected is elided then it is disabled by default
                .orElse(false)

        if (myAllFileTypesSelectedAlreadySet) {
            // If the user has already configured IntelliJ to format all file types and turned on formatting on save,
            // we leave the configuration as is as it will format java code, and we don't want to disable formatting
            // for other file types
            return
        }

        // Otherwise we setup intellij to not format all files...
        matchOrCreateChild(onSaveOptions, 'option', [name: 'myAllFileTypesSelected']).attributes().put('value', 'false')

        // ...but ensure java is formatted
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
