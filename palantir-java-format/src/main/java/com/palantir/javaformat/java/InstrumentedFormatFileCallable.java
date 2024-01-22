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
package com.palantir.javaformat.java;

import com.palantir.javaformat.java.InstrumentedFormatFileCallable.FormatFileResult;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.immutables.value.Value.Immutable;

/**
 * Instrumentation on top of {@link FormatFileCallable} to track the time spent formatting a given file, and produce
 * warnings
 */
public class InstrumentedFormatFileCallable implements Callable<FormatFileResult> {

    private final FormatFileCallable delegate;

    public InstrumentedFormatFileCallable(FormatFileCallable delegate) {
        this.delegate = delegate;
    }

    @Override
    public FormatFileResult call() throws Exception {
        long start = System.currentTimeMillis();
        String result = delegate.call();
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
        if (duration.toMillis() > Duration.ofSeconds(1).toMillis()) {
            throw new IllegalArgumentException("Formatting took too long: " + duration.toMillis() + "ms");
        }
        return FormatFileResult.of(result, duration);
    }

    @Immutable
    public interface FormatFileResult {
        String result();

        Duration duration();

        static FormatFileResult of(String result, Duration duration) {
            return ImmutableFormatFileResult.builder()
                    .result(result)
                    .duration(duration)
                    .build();
        }
    }
}
