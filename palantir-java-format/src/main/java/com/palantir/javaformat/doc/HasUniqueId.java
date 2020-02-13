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
