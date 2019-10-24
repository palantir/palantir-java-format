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

package com.palantir.javaformat.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;

public final class ParameterizedClass implements TestTemplateInvocationContextProvider {
    private static ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(ParameterizedClass.class);

    /**
     * Annotation for a method which provides parameters to be injected into the test class constructor by <code>
     * Parameterized</code>. The method has to be public and static.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Parameters {
        /**
         * Optional pattern to derive the test's name from the parameters. Use numbers in braces to refer to the
         * parameters or the additional data as follows:
         *
         * <pre>
         * {index} - the current parameter index
         * {0} - the first parameter value
         * {1} - the second parameter value
         * etc...
         * </pre>
         *
         * <p>
         *
         * @see MessageFormat
         */
        String name() default "{index}";
    }

    /**
     * Annotation for fields of the test class which will be initialized by the method annotated by <code>Parameters
     * </code>. By using directly this annotation, the test class constructor isn't needed.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Parameter {
        /**
         * The index of the parameter in the array returned by the method annotated by <code>Parameters</code>. Index
         * range must start at 0.
         */
        int value() default 0;
    }

    /**
     * For every method annotated with @TestTemplate, this guy gets called. We can cause the users' @TestTemplate method
     * to be invoked n times by returning n things.
     */
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {

        // we cache the result of our @Parameters in the 'parent' so it can be reused by other @TestTemplate methods
        ExtensionContext parent = extensionContext.getParent().get();

        List<Object[]> objectArrayArray = invokeUserParametersMethod(parent, parent.getTestClass().get());
        String stringFormatTemplate = findStringFormatTemplate(parent, parent.getTestClass().get());

        return objectArrayArray.stream().map(objectArray -> new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                String replaced = stringFormatTemplate.replaceAll("\\{index\\}", Integer.toString(invocationIndex));
                return MessageFormat.format(replaced, objectArray);
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return Arrays.asList(new InjectFields(objectArray), new InjectConstructorParameters(objectArray));
            }
        });
    }

    /** Fills in constructor params using values from the @Parameters method. */
    private static final class InjectConstructorParameters implements ParameterResolver {
        private final Object[] objectArray;

        InjectConstructorParameters(Object[] objectArray) {
            this.objectArray = objectArray;
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext _extensionContext)
                throws ParameterResolutionException {
            return (parameterContext.getDeclaringExecutable() instanceof Constructor)
                    && parameterContext.getIndex() < objectArray.length;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext _extensionContext)
                throws ParameterResolutionException {
            return objectArray[parameterContext.getIndex()];
        }
    }

    /** Mutates an instance of a testClass to fill in all the fields annotated with @Parameter. */
    private static final class InjectFields implements BeforeEachCallback {
        private final Object[] objectArray;

        private InjectFields(Object[] objectArray) {
            this.objectArray = objectArray;
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            List<Field> annotatedFields = context.getTestClass()
                    .map(clazz -> AnnotationUtils.findAnnotatedFields(clazz, Parameter.class, field -> true))
                    .orElseGet(Collections::emptyList);

            Object testClassInstance = context.getTestInstance().get();
            for (Field annotatedField : annotatedFields) {
                Parameter parameter = AnnotationUtils.findAnnotation(annotatedField, Parameter.class).get();
                int indexIntoArray = parameter.value();
                annotatedField.set(testClassInstance, objectArray[indexIntoArray]);
            }
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext _value) {
        return true; // parameterized everything annotated with @TestTemplate
    }

    /** Users must provide a public static Object[][] method, and this invokes it *once*, caching the result. */
    private static List<Object[]> invokeUserParametersMethod(ExtensionContext extensionContext, Class<?> testClass) {
        List<Method> methods = AnnotationUtils.findAnnotatedMethods(
                testClass, Parameters.class, ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP);

        Method userParametersMethod = methods.get(0);

        return extensionContext
                .getStore(namespace)
                .getOrComputeIfAbsent(
                        "invokeUserParametersMethod", unused -> allParameters(userParametersMethod), List.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> allParameters(Method method) {
        try {
            Object parameters = method.invoke(null);
            if (parameters instanceof List) {
                return (List<Object>) parameters;
            } else if (parameters instanceof Collection) {
                return new ArrayList<>((Collection<Object>) parameters);
            } else if (parameters instanceof Iterable) {
                List<Object> result = new ArrayList<>();
                for (Object entry : (Iterable<Object>) parameters) {
                    result.add(entry);
                }
                return result;
            } else if (parameters instanceof Object[]) {
                return Arrays.asList((Object[]) parameters);
            } else {
                throw new TestInstantiationException("Invalid return type. Must be iterable of arrays");
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new TestInstantiationException("barf", e);
        }
    }

    private static String findStringFormatTemplate(ExtensionContext extensionContext, Class<?> testClass) {
        return extensionContext
                .getStore(namespace)
                .getOrComputeIfAbsent(
                        "findStringFormatTemplate",
                        unused -> {
                            List<Method> methods = AnnotationUtils.findAnnotatedMethods(
                                    testClass, Parameters.class, ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP);

                            Method method = methods.get(0);

                            return AnnotationUtils.findAnnotation(method, Parameters.class).get().name();
                        },
                        String.class);
    }
}
