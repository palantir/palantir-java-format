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

/**
 * Each {@link Break} in a {@link Level} is either {@link FillMode#UNIFIED} or {@link
 * FillMode#INDEPENDENT}.
 */
public enum FillMode {
  /**
   * If a {@link Level} will not fit on one line, all of its {@code UNIFIED} {@link Break}s will be
   * broken.
   */
  UNIFIED,

  /**
   * If a {@link Level} will not fit on one line, its {@code INDEPENDENT} {@link Break}s will be
   * broken independently of each other, to fill in the {@link Level}.
   */
  INDEPENDENT,

  /**
   * A {@code FORCED} {@link Break} will always be broken, and a {@link Level} it appears in will
   * not fit on one line.
   */
  FORCED
}
