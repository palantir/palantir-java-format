package com.palantir.javaformat.doc;

import fj.Ord;
import java.util.concurrent.atomic.AtomicInteger;

abstract class HasUniqueId {
    private static final AtomicInteger UNIQUE_ID_GENERATOR = new AtomicInteger();

    final int uniqueId = UNIQUE_ID_GENERATOR.incrementAndGet();

    static <D extends HasUniqueId> Ord<D> ord() {
        return Ord.on((D obj) -> obj.uniqueId, Ord.intOrd).ord();
    }
}
