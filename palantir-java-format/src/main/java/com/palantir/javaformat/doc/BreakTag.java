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
package com.palantir.javaformat.doc;

import com.google.errorprone.annotations.Immutable;

/**
 * Unique identifier for a break. A BreakTag can correspond to one or more {@link Break breaks}, and the state of the
 * BreakTag is determined by whether any of the breaks were taken. Then, conditional structures like
 * {@link com.palantir.javaformat.OpsBuilder.BlankLineWanted.ConditionalBlankLine} and
 * {@link com.palantir.javaformat.Indent.If} behave differently based on whether this BreakTag was 'broken' or not.
 *
 * @see State#wasBreakTaken
 */
@Immutable
public final class BreakTag extends HasUniqueId {}
