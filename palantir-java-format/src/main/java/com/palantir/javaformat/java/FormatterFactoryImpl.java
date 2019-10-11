package com.palantir.javaformat.java;

import com.google.auto.service.AutoService;

@AutoService(FormatterFactory.class)
public final class FormatterFactoryImpl implements FormatterFactory {
    @Override
    public FormatterService createFormatter(JavaFormatterOptions options) {
        return new Formatter(options);
    }
}
