package com.palantir.javaformat.java;

import org.immutables.value.Value;

@Value.Immutable
public interface JavaFormatterInternalOptions {

    @Value.Default
    default boolean reformatJavadoc() {
        return false;
    }

    class Builder extends ImmutableJavaFormatterInternalOptions.Builder {}

    static Builder builder() {
        return new Builder();
    }
}
