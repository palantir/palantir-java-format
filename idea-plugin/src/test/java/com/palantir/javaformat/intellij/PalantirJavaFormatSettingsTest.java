/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javaformat.intellij.PalantirJavaFormatSettings.State;
import org.junit.jupiter.api.Test;

public final class PalantirJavaFormatSettingsTest {

    @Test
    void skipReflowingLongStrings_defaultIsFalse() {
        State state = new State();
        assertThat(state.skipReflowingLongStrings).isFalse();
    }

    @Test
    void skipReflowingLongStrings_canBeSetToTrue() {
        State state = new State();
        state.skipReflowingLongStrings = true;
        assertThat(state.skipReflowingLongStrings).isTrue();
    }

    @Test
    void skipReflowingLongStrings_canBeSetToFalse() {
        State state = new State();
        state.skipReflowingLongStrings = true;
        state.skipReflowingLongStrings = false;
        assertThat(state.skipReflowingLongStrings).isFalse();
    }

    @Test
    void skipReflowingLongStrings_persistsAcrossStateInstances() {
        State state1 = new State();
        state1.skipReflowingLongStrings = true;

        State state2 = new State();
        state2.skipReflowingLongStrings = state1.skipReflowingLongStrings;

        assertThat(state2.skipReflowingLongStrings).isTrue();
    }
}
