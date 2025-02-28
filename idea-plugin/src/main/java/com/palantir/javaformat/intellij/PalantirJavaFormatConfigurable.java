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
import com.palantir.javaformat.intellij.PalantirJavaFormatSettings.EnabledState;
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

    @SuppressWarnings("for-rollout:RawTypes")
    private JComboBox styleComboBox;

    private JLabel isUsingNativeImage;
    private JLabel formatterVersion;
    private JLabel pluginVersion;

    @SuppressWarnings("for-rollout:NullAway")
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
        pluginVersion.setText(settings.getImplementationVersion().orElse("unknown"));
        formatterVersion.setText(getFormatterVersionText(settings));
        isUsingNativeImage.setText(isUsingNativeImage(settings));
    }

    @Override
    public boolean isModified() {
        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(project);
        return enable.isSelected() != settings.isEnabled()
                || !styleComboBox.getSelectedItem().equals(UiFormatterStyle.convert(settings.getStyle()));
    }

    @Override
    public void disposeUIResources() {}

    private static String isUsingNativeImage(PalantirJavaFormatSettings settings) {
        if (settings.getNativeImageClassPath().isPresent()) {
            return "Native image formatter (`palantir.native.formatter` gradle property is enabled)";
        } else {
            return "(Default setup) Java-based formatter";
        }
    }

    private static String getFormatterVersionText(PalantirJavaFormatSettings settings) {
        String suffix = settings.injectedVersionIsOutdated() ? " (using bundled)" : " (using injected)";
        return settings.computeFormatterVersion().orElse("(bundled)") + suffix;
    }

    @SuppressWarnings("for-rollout:UnusedMethod")
    private void createUIComponents() {
        styleComboBox = new ComboBox<>(UiFormatterStyle.values());
    }
}
