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

import com.google.common.base.Strings;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.palantir.javaformat.java.FormatterService;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.sls.versions.OrderableSlsVersion;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

@State(
        name = "PalantirJavaFormatSettings",
        storages = {@Storage("palantir-java-format.xml")})
class PalantirJavaFormatSettings implements PersistentStateComponent<PalantirJavaFormatSettings.State> {

    private State state = new State();

    static PalantirJavaFormatSettings getInstance(Project project) {
        return ServiceManager.getService(project, PalantirJavaFormatSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(State state) {
        this.state = state;
    }

    boolean isEnabled() {
        return state.enabled.equals(EnabledState.ENABLED);
    }

    void setEnabled(boolean enabled) {
        setEnabled(enabled ? EnabledState.ENABLED : EnabledState.DISABLED);
    }

    void setEnabled(EnabledState enabled) {
        state.enabled = enabled;
    }

    boolean isFormatJavadoc() {
        return state.formatJavadoc;
    }

    void setFormatJavadoc(boolean formatJavadoc) {
        state.formatJavadoc = formatJavadoc;
    }

    boolean isUninitialized() {
        return state.enabled.equals(EnabledState.UNKNOWN);
    }

    JavaFormatterOptions.Style getStyle() {
        return state.style;
    }

    void setStyle(JavaFormatterOptions.Style style) {
        state.style = style;
    }

    /**
     * The paths to jars that provide an alternative implementation of the formatter. If set, this implementation will
     * be used instead of the bundled version.
     */
    Optional<List<URI>> getImplementationClassPath() {
        return state.implementationClassPath;
    }

    boolean injectedVersionIsOutdated() {
        Optional<String> formatterVersion = computeFormatterVersion();
        Optional<OrderableSlsVersion> implementationVersion = OrderableSlsVersion.safeValueOf(
                getImplementationVersion().map(v -> v.replace(".dirty", "")).orElse(""));

        if (formatterVersion.isEmpty() || implementationVersion.isEmpty()) {
            return true;
        }

        OrderableSlsVersion injectedVersion = OrderableSlsVersion.valueOf(formatterVersion.get());
        return injectedVersion.compareTo(implementationVersion.get()) < 0;
    }

    Optional<String> getImplementationVersion() {
        return Optional.ofNullable(Strings.emptyToNull(
                PalantirJavaFormatConfigurable.class.getPackage().getImplementationVersion()));
    }

    Optional<String> computeFormatterVersion() {
        return getImplementationClassPath().map(classpath -> classpath.stream()
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
                .orElseThrow(() -> new RuntimeException("Couldn't find implementation JAR")));
    }

    enum EnabledState {
        UNKNOWN,
        ENABLED,
        DISABLED
    }

    static class State {

        private EnabledState enabled = EnabledState.UNKNOWN;
        private Optional<List<URI>> implementationClassPath = Optional.empty();

        private boolean formatJavadoc = false;
        public JavaFormatterOptions.Style style = JavaFormatterOptions.Style.PALANTIR;

        public void setImplementationClassPath(@Nullable List<String> value) {
            implementationClassPath = Optional.ofNullable(value)
                    .map(strings -> strings.stream().map(URI::create).collect(Collectors.toList()));
        }

        public List<String> getImplementationClassPath() {
            return implementationClassPath
                    .map(paths -> paths.stream().map(URI::toString).collect(Collectors.toList()))
                    .orElse(null);
        }

        // enabled used to be a boolean so we use bean property methods for backwards compatibility
        public void setEnabled(@Nullable String enabledStr) {
            if (enabledStr == null) {
                enabled = EnabledState.UNKNOWN;
            } else if (Boolean.valueOf(enabledStr)) {
                enabled = EnabledState.ENABLED;
            } else {
                enabled = EnabledState.DISABLED;
            }
        }

        public String getEnabled() {
            switch (enabled) {
                case ENABLED:
                    return "true";
                case DISABLED:
                    return "false";
                default:
                    return null;
            }
        }

        public boolean isFormatJavadoc() {
            return formatJavadoc;
        }

        public void setFormatJavadoc(boolean formatJavadoc) {
            this.formatJavadoc = formatJavadoc;
        }

        @Override
        public String toString() {
            return "PalantirJavaFormatSettings{"
                    + "enabled="
                    + enabled
                    + ", formatJavadoc="
                    + formatJavadoc
                    + ", formatterPath="
                    + implementationClassPath
                    + ", style="
                    + style
                    + '}';
        }
    }
}
