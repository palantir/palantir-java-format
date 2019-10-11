package com.palantir.javaformat.java;

public interface FormatterFactory {
    FormatterService createFormatter(JavaFormatterOptions options);
}
