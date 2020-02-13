package com.palantir.javaformat.doc;

import com.google.errorprone.annotations.Immutable;
import java.util.function.Supplier;

@Immutable
interface ImmutableSupplier<T> extends Supplier<T> {}
