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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.files.arbitrary.ArbitraryFile;
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.io.File;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@GradlePluginTests
@DisabledConfigurationCache
class PalantirJavaFormatIdeaPluginTest {

    private static final File NATIVE_IMAGE_FILE = new File("build/nativeImage.path");

    private static final String NATIVE_CONFIG =
            String.format("palantirJavaFormatNative files(\"%s\")", NATIVE_IMAGE_FILE);

    @ParameterizedTest(name = "extraGradleProperties={0}")
    @ValueSource(strings = {"", "palantir.native.formatter=true"})
    void idea_configures_xml_files(String extraGradleProperties, GradleInvoker gradle, RootProject rootProject)
            throws Exception {

        rootProject.gradlePropertiesFile().appendLine(extraGradleProperties);

        rootProject.buildGradle().plugins().add("com.palantir.java-format-idea").add("idea");

        rootProject.buildGradle().append("""
            dependencies {
                palantirJavaFormat project.files() // no need to store the real thing in here
                %s
            }
            """, extraGradleProperties.isBlank() ? "" : NATIVE_CONFIG);

        gradle.withArgs("idea").buildsSuccessfully();

        ArbitraryFile pjfXmlFile = rootProject.file(".idea/palantir-java-format.xml");
        pjfXmlFile.assertThat().exists();

        Document xmlContent = parseXml(pjfXmlFile.path().toFile());
        NodeList settings = xmlContent.getElementsByTagName("component");
        boolean foundSettings = false;
        for (int i = 0; i < settings.getLength(); i++) {
            Element element = (Element) settings.item(i);
            if ("PalantirJavaFormatSettings".equals(element.getAttribute("name"))) {
                foundSettings = true;
                break;
            }
        }
        assertThat(foundSettings)
                .describedAs("Should find PalantirJavaFormatSettings component")
                .isTrue();

        NodeList options = xmlContent.getElementsByTagName("option");
        boolean foundImplementationClassPath = false;
        boolean foundNativeImageClassPath = false;
        for (int i = 0; i < options.getLength(); i++) {
            Element element = (Element) options.item(i);
            if ("implementationClassPath".equals(element.getAttribute("name"))) {
                foundImplementationClassPath = true;
            }
            if ("nativeImageClassPath".equals(element.getAttribute("name"))) {
                foundNativeImageClassPath = true;
            }
        }
        assertThat(foundImplementationClassPath)
                .describedAs("Should find implementationClassPath option")
                .isTrue();
        if (extraGradleProperties.contains("palantir.native.formatter=true")) {
            assertThat(foundNativeImageClassPath)
                    .describedAs("Should find nativeImageClassPath option when native formatter is enabled")
                    .isTrue();
        }

        ArbitraryFile workspaceXmlFile = rootProject.file(".idea/workspace.xml");
        workspaceXmlFile.assertThat().exists();

        Document workspaceContent = parseXml(workspaceXmlFile.path().toFile());
        NodeList workspaceComponents = workspaceContent.getElementsByTagName("component");

        boolean foundFormatOnSave = false;
        boolean foundOptimizeOnSave = false;
        for (int i = 0; i < workspaceComponents.getLength(); i++) {
            Element element = (Element) workspaceComponents.item(i);
            if ("FormatOnSaveOptions".equals(element.getAttribute("name"))) {
                foundFormatOnSave = true;
            }
            if ("OptimizeOnSaveOptions".equals(element.getAttribute("name"))) {
                foundOptimizeOnSave = true;
            }
        }
        assertThat(foundFormatOnSave)
                .describedAs("Should find FormatOnSaveOptions component in workspace.xml")
                .isTrue();
        assertThat(foundOptimizeOnSave)
                .describedAs("Should find OptimizeOnSaveOptions component in workspace.xml")
                .isTrue();
    }

    private static Document parseXml(File file) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }
}
