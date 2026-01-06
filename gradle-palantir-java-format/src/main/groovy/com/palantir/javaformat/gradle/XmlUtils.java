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

package com.palantir.javaformat.gradle;

import groovy.util.Node;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class XmlUtils {

    public static void updateIdeaXmlFile(File configurationFile, Consumer<Node> configure) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc;
            if (configurationFile.isFile()) {
                doc = builder.parse(configurationFile);
            } else {
                doc = builder.newDocument();
                Element root = doc.createElement("project");
                root.setAttribute("version", "4");
                doc.appendChild(root);
            }

            Node groovyNode = convertDomToGroovyNode(doc.getDocumentElement(), null);
            configure.accept(groovyNode);

            Document newDoc = convertGroovyNodeToDom(groovyNode);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            DOMSource source = new DOMSource(newDoc);
            StreamResult result = new StreamResult(configurationFile);
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update XML file: " + configurationFile, e);
        }
    }

    private static Node convertDomToGroovyNode(org.w3c.dom.Node domNode, Node parent) {
        if (domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
            return null;
        }

        Element element = (Element) domNode;
        Map<String, String> attributes = new LinkedHashMap<>();

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            org.w3c.dom.Node attr = attrs.item(i);
            attributes.put(attr.getNodeName(), attr.getNodeValue());
        }

        Node groovyNode = new Node(parent, element.getTagName(), attributes);

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                convertDomToGroovyNode(child, groovyNode);
            } else if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                String text = child.getNodeValue();
                if (text != null && !text.trim().isEmpty()) {
                    groovyNode.setValue(text);
                }
            }
        }

        return groovyNode;
    }

    @SuppressWarnings("unchecked")
    private static Document convertGroovyNodeToDom(Node groovyNode) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = convertNodeToDomElement(doc, groovyNode);
        doc.appendChild(root);

        return doc;
    }

    @SuppressWarnings("unchecked")
    private static Element convertNodeToDomElement(Document doc, Node groovyNode) {
        Element element = doc.createElement(groovyNode.name().toString());

        Map<String, Object> attributes = (Map<String, Object>) groovyNode.attributes();
        attributes.forEach((key, value) -> element.setAttribute(key, String.valueOf(value)));

        Object value = groovyNode.value();
        if (value instanceof Collection<?> children) {
            for (Object child : children) {
                if (child instanceof Node childNode) {
                    element.appendChild(convertNodeToDomElement(doc, childNode));
                } else if (child instanceof String childString) {
                    String text = childString.trim();
                    if (!text.isEmpty()) {
                        element.setTextContent(text);
                    }
                }
            }
        } else if (value instanceof String strValue) {
            String text = strValue.trim();
            if (!text.isEmpty()) {
                element.setTextContent(text);
            }
        }

        return element;
    }

    private XmlUtils() {}
}
