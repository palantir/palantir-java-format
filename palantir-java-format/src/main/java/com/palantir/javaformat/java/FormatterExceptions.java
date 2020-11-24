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

import static java.util.Locale.ENGLISH;

import com.google.common.collect.Iterables;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

final class FormatterExceptions {

    static FormatterException fromJavacDiagnostics(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics) {
        return new FormatterException(Iterables.transform(diagnostics, FormatterExceptions::toFormatterDiagnostic));
    }

    private static FormatterDiagnostic toFormatterDiagnostic(Diagnostic<?> input) {
        return FormatterDiagnostic.create(
                (int) input.getLineNumber(), (int) input.getColumnNumber(), input.getMessage(ENGLISH));
    }

    private FormatterExceptions() {}
}
