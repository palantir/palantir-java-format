/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.palantir.javaformat.java;

import com.google.errorprone.annotations.Immutable;

/**
 * Options for a google-java-format invocation.
 *
 * <p>Like gofmt, the google-java-format CLI exposes <em>no</em> configuration options (aside from {@code --aosp}).
 *
 * <p>The goal of google-java-format is to provide consistent formatting, and to free developers from arguments over
 * style choices. It is an explicit non-goal to support developers' individual preferences, and in fact it would work
 * directly against our primary goals.
 */
@Immutable
public final class JavaFormatterOptions {

    public enum Style {
        /** The default Palantir Java Style configuration. */
        PALANTIR(2, 120),

        /** The default Google Java Style configuration. */
        GOOGLE(1, 100),

        /** The AOSP-compliant configuration. */
        AOSP(2, 100);

        private final int indentationMultiplier;
        private final int maxLineLength;

        Style(int indentationMultiplier, int maxLineLength) {
            this.indentationMultiplier = indentationMultiplier;
            this.maxLineLength = maxLineLength;
        }

        int indentationMultiplier() {
            return indentationMultiplier;
        }

        public int maxLineLength() {
            return maxLineLength;
        }
    }

    private final Style style;

    private final boolean formatJavadoc;

    private JavaFormatterOptions(Style style, boolean formatJavadoc) {
        this.style = style;
        this.formatJavadoc = formatJavadoc;
    }

    /** Returns the multiplier for the unit of indent. */
    public int indentationMultiplier() {
        return style.indentationMultiplier();
    }

    public int maxLineLength() {
        return style.maxLineLength();
    }

    public boolean formatJavadoc() {
        return formatJavadoc;
    }

    /** Returns the code style. */
    public Style style() {
        return style;
    }

    /** Returns the default formatting options. */
    public static JavaFormatterOptions defaultOptions() {
        return builder().build();
    }

    /** Returns a builder for {@link JavaFormatterOptions}. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder for {@link JavaFormatterOptions}. */
    public static final class Builder {
        // default is still GOOGLE just because lots of hand-rolled tests rely on this behaviour
        private Style style = Style.GOOGLE;

        private boolean formatJavadoc = false;

        private Builder() {}

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder formatJavadoc(boolean formatJavadoc) {
            this.formatJavadoc = formatJavadoc;
            return this;
        }

        public JavaFormatterOptions build() {
            return new JavaFormatterOptions(style, formatJavadoc);
        }
    }
}
