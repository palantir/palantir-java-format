package com.palantir.javaformat.doc;

import fj.Ord;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class exists in order to provide a stable {@link Ord} implementation for {@link Doc} and {@link BreakTag}, so
 * that we can store them inside ordered immutable types like {@link fj.data.Set} and {@link fj.data.HashMap}.
 */
abstract class HasUniqueId {
    private static final AtomicInteger UNIQUE_ID_GENERATOR = new AtomicInteger();

    final int uniqueId = UNIQUE_ID_GENERATOR.incrementAndGet();

    static <D extends HasUniqueId> Ord<D> ord() {
        return Ord.on((D obj) -> obj.uniqueId, Ord.intOrd).ord();
    }
}
