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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.palantir.javaformat.java.JavaFormatterOptions;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@State(
        name = "PalantirJavaFormatSettings",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
class PalantirJavaFormatSettings implements PersistentStateComponent<PalantirJavaFormatSettings.State> {
    private static final Logger log = LoggerFactory.getLogger(PalantirJavaFormatSettings.class);

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
        log.info("Loaded new state: {}", state);
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

    boolean isUninitialized() {
        return state.enabled.equals(EnabledState.UNKNOWN);
    }

    JavaFormatterOptions.Style getStyle() {
        return state.style;
    }

    void setStyle(JavaFormatterOptions.Style style) {
        state.style = style;
    }

    enum EnabledState {
        UNKNOWN,
        ENABLED,
        DISABLED;
    }

    static class State {

        private EnabledState enabled = EnabledState.ENABLED;
        private Optional<Path> formatterPath = Optional.empty();
        public JavaFormatterOptions.Style style = JavaFormatterOptions.Style.PALANTIR;

        public void setFormatterPath(@Nullable String value) {
            formatterPath = Optional.ofNullable(value).map(Paths::get);
        }

        public String getFormatterPath() {
            return formatterPath.map(Path::toString).orElse(null);
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

        @Override
        public String toString() {
            return "State{" + "enabled=" + enabled + ", formatterPath=" + formatterPath + ", style=" + style + '}';
        }
    }
}
