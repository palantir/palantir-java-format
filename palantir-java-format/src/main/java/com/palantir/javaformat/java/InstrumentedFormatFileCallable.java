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
