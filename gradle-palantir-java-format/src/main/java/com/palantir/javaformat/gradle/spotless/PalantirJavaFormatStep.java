package com.palantir.javaformat.gradle.spotless;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.google.common.base.Suppliers;
import com.palantir.javaformat.gradle.JavaFormatExtension;
import com.palantir.javaformat.java.FormatterService;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.function.Supplier;
import org.gradle.api.artifacts.Configuration;

public final class PalantirJavaFormatStep {

    private static final String IMPL_CLASS = "com.palantir.javaformat.java.Formatter";

    private PalantirJavaFormatStep() {}

    private static final String NAME = "palantir-java-format";

    /** Creates a step which formats everything - code, import order, and unused imports. */
    public static FormatterStep create(Configuration palantirJavaFormat, JavaFormatExtension extension) {
        ensureImplementationNotDirectlyLoadable();
        Supplier<FormatterService> memoizedService = Suppliers.memoize(extension::serviceLoad);
        return FormatterStep.createLazy(
                NAME, () -> new State(palantirJavaFormat.getFiles(), memoizedService), State::createFormat);
    }

    static final class State implements Serializable {
        private static final long serialVersionUID = 1L;

        // Kept for state serialization purposes.
        @SuppressWarnings("unused")
        private final String stepName = NAME;

        // Kept for state serialization purposes.
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final FileSignature jarsSignature;

        // Transient as this is not serializable.
        private final transient Supplier<FormatterService> memoizedFormatter;

        /**
         * Build a cacheable state for spotless from the given jars, that uses the given {@link FormatterService}.
         *
         * @param jars The jars that contain the palantir-java-format implementation. This is only used for caching and
         *     up-to-dateness purposes.
         */
        State(Iterable<File> jars, Supplier<FormatterService> memoizedFormatter) throws IOException {
            this.jarsSignature = FileSignature.signAsSet(jars);
            this.memoizedFormatter = memoizedFormatter;
        }

        @SuppressWarnings("NullableProblems")
        FormatterFunc createFormat() {
            return memoizedFormatter.get()::formatSourceReflowStringsAndFixImports;
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
