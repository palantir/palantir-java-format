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

import com.fasterxml.jackson.annotation.JsonProperty;
import fj.Ord;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class exists in order to provide a stable {@link Ord} implementation for {@link Doc} and {@link BreakTag}, so
 * that we can store them inside ordered immutable types like {@link fj.data.Set} and {@link fj.data.HashMap}.
 */
public abstract class HasUniqueId {
    private static final AtomicInteger UNIQUE_ID_GENERATOR = new AtomicInteger();

    private final int uniqueId = UNIQUE_ID_GENERATOR.incrementAndGet();

    @JsonProperty("id")
    public final int id() {
        return uniqueId;
    }

    static <D extends HasUniqueId> Ord<D> ord() {
        return Ord.on((D obj) -> obj.id(), Ord.intOrd).ord();
    }
}
