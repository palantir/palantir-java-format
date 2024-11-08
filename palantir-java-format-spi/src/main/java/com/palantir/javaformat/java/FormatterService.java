/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.javaformat.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.function.Function;

/**
 * A stable facade for palantir-java-format. The implementation must be ServiceLoaded, to ensure its classpath remains
 * isolated.
 */
public interface FormatterService {

    /**
     * Emit a list of {@link Replacement}s to convert from input to formatted output.
     *
     * @param input the input compilation unit
     * @param ranges the character ranges to reformat
     * @return a list of {@link Replacement}s, sorted from low index to high index, without overlaps
     * @throws FormatterException if the input string cannot be parsed
     */
    ImmutableList<Replacement> getFormatReplacements(String input, Collection<Range<Integer>> ranges)
            throws FormatterException;

    /**
     * Formats an input string (a Java compilation unit), reflows strings and fixes imports.
     *
     * <p>Fixing imports includes ordering, spacing, and removal of unused import statements.
     *
     * @param input the input string
     * @return the output string
     * @throws FormatterException if the input string cannot be parsed
     */
    String formatSourceReflowStringsAndFixImports(String input) throws FormatterException;

    /**
     * Derives a new formatter service from this service with the given options. Note
     * that formatter services are immutable, this instance can still be used with the
     * old settings.
     * <p>
     * Note: The default implementation exists for backwards compatibility and simply returns
     * this instance.
     * @param optionsTransformer An operator that is given the current set of options and returns the new set of
     * options. You can either ignore the given options and build a new set of options from scratch, or use
     * {@link JavaFormatterOptions.Builder#from(JavaFormatterOptions) JavaFormatterOptions.Builder.from()} to modify
     * only some options.
     * @return A formatter service that formats code with the given options.
     */
    default FormatterService withOptions(
            Function<? super JavaFormatterOptions, ? extends JavaFormatterOptions> optionsTransformer) {
        return this;
    }

    /**
     * Fixes imports (eg. ordering, spacing, and removal of unused import statements).
     *
     * @param input the input string
     * @return the output string
     * @throws FormatterException if the input string cannot be parsed
     */
    String fixImports(String input) throws FormatterException;
}
