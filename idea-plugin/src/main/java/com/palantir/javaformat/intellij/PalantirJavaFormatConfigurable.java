/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.palantir.javaformat.intellij;

import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.palantir.javaformat.intellij.PalantirJavaFormatSettings.EnabledState;
import com.palantir.javaformat.java.FormatterService;
import java.awt.Insets;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

class PalantirJavaFormatConfigurable extends BaseConfigurable implements SearchableConfigurable {

    private final Project project;
    private JPanel panel;
    private JCheckBox enable;
    private JComboBox styleComboBox;
    private JLabel formatterVersion;
    private JLabel pluginVersion;

    public PalantirJavaFormatConfigurable(Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "palantir-java-format.settings";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "palantir-java-format Settings";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return panel;
    }

    @Override
    public void apply() throws ConfigurationException {
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        settings.setEnabled(enable.isSelected() ? EnabledState.ENABLED : getDisabledState());
        settings.setStyle(((UiFormatterStyle) styleComboBox.getSelectedItem()).convert());
    }

    private EnabledState getDisabledState() {
        // The default settings (inherited by new projects) are either 'enabled' or
        // 'show notification'. There's no way to default new projects to disabled. If someone wants
        // that, we can add another checkbox, I suppose.
        return project.isDefault() ? EnabledState.UNKNOWN : EnabledState.DISABLED;
    }

    @Override
    public void reset() {
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        enable.setSelected(settings.isEnabled());
        styleComboBox.setSelectedItem(UiFormatterStyle.convert(settings.getStyle()));
        pluginVersion.setText(PalantirJavaFormatConfigurable.class.getPackage().getImplementationVersion());
        formatterVersion.setText(settings.getImplementationClassPath()
                .map(this::computeFormatterVersion)
                .orElse("(bundled)"));
    }

    private String computeFormatterVersion(List<URI> implementationClassPath) {
        return implementationClassPath.stream()
                .flatMap(uri -> {
                    try {
                        JarFile jar = new JarFile(uri.getPath());
                        // Identify the implementation jar by the service it produces.
                        if (jar.getEntry("META-INF/services/" + FormatterService.class.getName()) != null) {
                            String implementationVersion =
                                    jar.getManifest().getMainAttributes().getValue("Implementation-Version");
                            return Stream.of(implementationVersion);
                        }
                        return Stream.empty();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find implementation JAR"));
    }

    @Override
    public boolean isModified() {
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        return enable.isSelected() != settings.isEnabled()
                || !styleComboBox.getSelectedItem().equals(UiFormatterStyle.convert(settings.getStyle()));
    }

    @Override
    public void disposeUIResources() {}

    private void createUIComponents() {
        styleComboBox = new ComboBox<>(UiFormatterStyle.values());
    }

    {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
     * code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        panel = new JPanel();
        panel.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        enable = new JCheckBox();
        enable.setText("Enable palantir-java-format");
        panel.add(
                enable,
                new GridConstraints(
                        0,
                        0,
                        1,
                        2,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false));
        final Spacer spacer1 = new Spacer();
        panel.add(
                spacer1,
                new GridConstraints(
                        4,
                        0,
                        1,
                        2,
                        GridConstraints.ANCHOR_CENTER,
                        GridConstraints.FILL_VERTICAL,
                        1,
                        GridConstraints.SIZEPOLICY_WANT_GROW,
                        null,
                        null,
                        null,
                        0,
                        false));
        final JLabel label1 = new JLabel();
        label1.setText("Code style");
        panel.add(
                label1,
                new GridConstraints(
                        1,
                        0,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false));
        panel.add(
                styleComboBox,
                new GridConstraints(
                        1,
                        1,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        1,
                        false));
        final JLabel label2 = new JLabel();
        label2.setText("Implementation version");
        panel.add(
                label2,
                new GridConstraints(
                        3,
                        0,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false));
        formatterVersion = new JLabel();
        formatterVersion.setText("what version are we running with?");
        panel.add(
                formatterVersion,
                new GridConstraints(
                        3,
                        1,
                        1,
                        1,
                        GridConstraints.ANCHOR_CENTER,
                        GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        1,
                        false));
        final JLabel label3 = new JLabel();
        label3.setText("Plugin version");
        panel.add(
                label3,
                new GridConstraints(
                        2,
                        0,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false));
        pluginVersion = new JLabel();
        pluginVersion.setText("plugin version");
        panel.add(
                pluginVersion,
                new GridConstraints(
                        2,
                        1,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        1,
                        false));
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }
}
