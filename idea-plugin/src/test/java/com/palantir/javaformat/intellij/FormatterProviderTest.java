package com.palantir.javaformat.intellij;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public final class FormatterProviderTest {

    @Test
    void testParseSdkJavaVersion_major() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15")).isEqualTo(15);
    }

    @Test
    void testParseSdkJavaVersion_majorMinorPatch() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15.0.2")).isEqualTo(15);
    }

    @Test
    void testParseSdkJavaVersion_ea() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15-ea")).isEqualTo(15);
    }
}
