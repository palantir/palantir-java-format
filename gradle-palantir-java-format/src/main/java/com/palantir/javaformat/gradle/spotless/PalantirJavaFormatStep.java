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
package com.palantir.javaformat.gradle.spotless;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.function.Supplier;
import org.gradle.api.artifacts.Configuration;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public final class PalantirJavaFormatStep {

    private static final String IMPL_CLASS = "com.palantir.javaformat.java.Formatter";

    private PalantirJavaFormatStep() {}

    private static final String NAME = "palantir-java-format";

    /**
     * Creates a step which formats everything - code, import order, and unused imports, using the Worker API for
     * process isolation.
     */
    public static FormatterStep create(Configuration palantirJavaFormat, WorkerExecutor workerExecutor) {
        ensureImplementationNotDirectlyLoadable();
        return FormatterStep.createLazy(
                NAME, () -> new State(palantirJavaFormat::getFiles, workerExecutor), State::createFormat);
    }

    static final class State implements Serializable {
        private static final long serialVersionUID = 1L;

        // Kept for state serialization purposes.
        @SuppressWarnings("unused")
        private final String stepName = NAME;

        // Spotless' `FormatterStepImpl` implements Java's `Serializable` interface in a weird way:
        // It serializes this `State` class[1].
        //
        // Gradle understands Java's `Serializable`, and uses it to invalidate `@Input`s to tasks.
        //
        // Since FormatterStepImpl is an input to `SpotlessTask`[2], anything serialized as part of this `State`
        // is used for up-to-date checking for the `SpotlessTask`
        //
        // [1]
        // https://github.com/diffplug/spotless/blob/52654ef8c4a6191d983b10a2370d53b1ca023f7d/lib/src/main/java/com/diffplug/spotless/LazyForwardingEquality.java#L68
        // [2]
        // https://github.com/diffplug/spotless/blob/f32701212bf8d327c67d10c35316cb80dcdf577b/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/SpotlessTask.java#L163
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private FileSignature jarsSignature;

        private final transient Supplier<Iterable<File>> jarsSupplier;

        // Transient as this is not serializable.
        private final transient WorkerExecutor workerExecutor;

        /**
         * Build a cacheable state for spotless from the given jars, using the Worker API for process isolation.
         *
         * @param jarsSupplier Supplies the jars that contain the palantir-java-format implementation. This is only used for caching and
         * up-to-dateness purposes.
         * @param workerExecutor WorkerExecutor for process isolation.
         */
        @SuppressWarnings("for-rollout:NullAway")
        State(Supplier<Iterable<File>> jarsSupplier, WorkerExecutor workerExecutor) {
            this.jarsSupplier = jarsSupplier;
            this.workerExecutor = workerExecutor;
        }

        @SuppressWarnings("NullableProblems")
        FormatterFunc createFormat() {
            return input -> {
                try {
                    // Only resolve the jars and compute the signature at execution time!
                    Iterable<File> jars = jarsSupplier.get();

                    // Not a performance issue, as Spotless caches this
                    // https://github.com/diffplug/spotless/blob/228eb10af382b19e130d8d9479f7a95238cb4358/lib/src/main/java/com/diffplug/spotless/FileSignature.java#L138-L143
                    this.jarsSignature = FileSignature.signAsSet(jars);

                    return formatWithWorker(input, jars);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }

        private String formatWithWorker(String input, Iterable<File> jars) throws IOException {
            // Create temp files for input/output communication with worker
            File inputFile =
                    Files.createTempFile("palantir-java-format-input-", ".java").toFile();
            File outputFile = Files.createTempFile("palantir-java-format-output-", ".java")
                    .toFile();

            try {
                Files.writeString(inputFile.toPath(), input);

                WorkQueue workQueue = workerExecutor.processIsolation(workerSpec -> {
                    workerSpec.getClasspath().from(jars);
                    workerSpec.forkOptions(options -> {
                        options.jvmArgs(
                                "--add-exports",
                                "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                                "--add-exports",
                                "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                                "--add-exports",
                                "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                                "--add-exports",
                                "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                                "--add-exports",
                                "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");
                    });
                });

                workQueue.submit(FormatJavaWorkAction.class, parameters -> {
                    parameters.getInputFile().set(inputFile);
                    parameters.getOutputFile().set(outputFile);
                    parameters.getFormatterClasspath().from(jars);
                });

                return Files.readString(outputFile.toPath());
            } catch (Exception e) {
                throw new RuntimeException("Formatting failed using Worker API", e);
            } finally {
                inputFile.delete();
                outputFile.delete();
            }
        }
    }

    private static void ensureImplementationNotDirectlyLoadable() {
        try {
            PalantirJavaFormatStep.class.getClassLoader().loadClass(IMPL_CLASS);
        } catch (ClassNotFoundException e) {
            // expected
            return;
        }
        throw new RuntimeException("Expected not be be able to load "
                + IMPL_CLASS
                + " via main class loader but was able to. Please ensure that `buildscript.configurations.classpath`"
                + " doesn't depend on `com.palantir.javaformat:palantir-java-format`.");
    }
}
