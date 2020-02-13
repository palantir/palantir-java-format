package com.palantir.javaformat.java;

import com.google.errorprone.annotations.Immutable;
import org.immutables.value.Value;

@Value.Immutable
@Immutable
interface JavaFormatterInternalOptions {

    @Value.Default
    default boolean reformatJavadoc() {
        return false;
    }

    class Builder extends ImmutableJavaFormatterInternalOptions.Builder {}

    static Builder builder() {
        return new Builder();
    }
}
