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

import groovy.util.Node;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConfigureJavaFormatterXml {

    static void configureJavaFormat(Node rootNode, List<URI> uris, Optional<URI> nativeImageUri) {
        Node settings = matchOrCreateChild(rootNode, "component", Map.of("name", "PalantirJavaFormatSettings"));
        // enable
        attributes(matchOrCreateChild(settings, "option", Map.of("name", "enabled")))
                .put("value", "true");
        // configure classpath
        Node classPath = matchOrCreateChild(settings, "option", Map.of("name", "implementationClassPath"));
        Node listItems = matchOrCreateChild(classPath, "list", /* attributes= */ Map.of());
        listItems.children().clear();
        uris.forEach(uri -> listItems.appendNode("option", new LinkedHashMap<>(Map.of("value", uri))));
        // configure nativeImageClasspath
        nativeImageUri.ifPresentOrElse(
                uri -> attributes(matchOrCreateChild(settings, "option", Map.of("name", "nativeImageClassPath")))
                        .put("value", uri),
                () -> matchChild(settings, "option", Map.of("name", "nativeImageClassPath"))
                        .ifPresent(node -> node.parent().remove(node)));
    }

    static void configureWorkspaceXml(Node rootNode) {
        configureFormatOnSave(rootNode);
        configureOptimizeOnSave(rootNode);
    }

    private static void configureFormatOnSave(Node rootNode) {
        configureOnSaveAction(matchOrCreateChild(rootNode, "component", Map.of("name", "FormatOnSaveOptions")));
    }

    private static void configureOptimizeOnSave(Node rootNode) {
        configureOnSaveAction(matchOrCreateChild(rootNode, "component", Map.of("name", "OptimizeOnSaveOptions")));
    }

    private static void configureOnSaveAction(Node onSaveOptions) {
        // In IntelliJ <2023.3.7, the myRunOnSave option is default false.
        // In IntelliJ >=2023.3.7, it is default true, and if it is set to true IntelliJ will remove it from the XML
        //     the next time the file is saved or the actions on save options window is saved.
        // So to cover all cases, we always write it out and ensure it is true
        Node myRunOnSave = matchOrCreateChild(onSaveOptions, "option", Map.of("name", "myRunOnSave"));
        attributes(myRunOnSave).put("value", "true");

        // In IntelliJ <2023.3.7, the myAllFileTypesSelected option is default true.
        // In IntelliJ >=2023.3.7, it is default false, and if it is set to false IntelliJ will remove it from the XML
        //     the next time the file is saved or the actions on save options window is saved.
        // People may have manually set this to true, so we respect that setting is so. However, if it doesn't exist,
        //     we make sure it is explicitly set false to work in all IntelliJ versions.
        matchOrCreateChild(onSaveOptions, "option", Map.of("name", "myAllFileTypesSelected"), Map.of("value", "false"));

        // Ensure that is per-file type settings are enabled, we ensure JAVA is part of the list.
        Node mySelectedFileTypes = matchOrCreateChild(onSaveOptions, "option", Map.of("name", "mySelectedFileTypes"));
        Node set = matchOrCreateChild(mySelectedFileTypes, "set", /* attributes= */ Map.of());
        matchOrCreateChild(set, "option", Map.of("value", "JAVA"));
    }

    private static Node matchOrCreateChild(Node base, String name, Map<String, ?> attributes) {
        return matchOrCreateChild(base, name, attributes, /* defaults= */ Map.of());
    }

    private static Node matchOrCreateChild(Node base, String name, Map<String, ?> attributes, Map<String, ?> defaults) {
        return matchChild(base, name, attributes).orElseGet(() -> {
            Map<String, Object> newAttributes = new LinkedHashMap<>(attributes);
            newAttributes.putAll(defaults);
            return base.appendNode(name, newAttributes);
        });
    }

    private static Optional<Node> matchChild(Node base, String name, Map<String, ?> attributes) {
        return ((List<?>) base.get(name))
                .stream()
                        .map(Node.class::cast)
                        .filter(child -> attributes(child).entrySet().containsAll(attributes.entrySet()))
                        .findFirst();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> attributes(Node node) {
        return node.attributes();
    }

    private ConfigureJavaFormatterXml() {}
}
