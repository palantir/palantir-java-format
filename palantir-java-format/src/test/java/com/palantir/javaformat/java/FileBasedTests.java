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

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public final class FileBasedTests {
    // Test files that are only used when run with a minimum Java version
    private static final ImmutableSet<String> JAVA_14_TESTS =
            ImmutableSet.of("ExpressionSwitch", "RSL", "Records", "Var", "I574", "I594");
    private static final ImmutableSet<String> JAVA_16_TESTS = ImmutableSet.of("I588");

    private final Class<?> testClass;
    /** The path prefix for all tests if loaded as resources. */
    private final Path resourcePrefix;
    /** Where to output test outputs when recreating. */
    private final Path fullTestPath;

    public FileBasedTests(Class<?> testClass) {
        this(testClass, testClass.getSimpleName());
    }

    public FileBasedTests(Class<?> testClass, String testDirName) {
        this.resourcePrefix =
                Paths.get(testClass.getPackage().getName().replace('.', '/')).resolve(testDirName);
        this.testClass = testClass;
        this.fullTestPath = Paths.get("src/test/resources").resolve(resourcePrefix);
    }

    public static void assumeJavaVersionForTest(String testName) {
        if (JAVA_14_TESTS.contains(testName)) {
            Assumptions.assumeTrue(Formatter.getRuntimeVersion() >= 14, "Not running on jdk 14 or later");
        } else if (JAVA_16_TESTS.contains(testName)) {
            Assumptions.assumeTrue(Formatter.getRuntimeVersion() >= 16, "Not running on jdk 16 or later");
        }
    }

    public List<Object[]> paramsAsNameInputOutput() throws IOException {
        ClassLoader classLoader = testClass.getClassLoader();
        Map<String, String> inputs = new TreeMap<>();
        Map<String, String> outputs = new TreeMap<>();
        for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
            String resourceName = resourceInfo.getResourceName();
            Path resourceNamePath = Paths.get(resourceName);
            if (resourceNamePath.startsWith(resourcePrefix)) {
                Path subPath = resourcePrefix.relativize(resourceNamePath);
                assertThat(subPath.getNameCount())
                        .describedAs("bad testdata file names")
                        .isEqualTo(1);
                String baseName = getNameWithoutExtension(subPath.getFileName().toString());
                String extension = getFileExtension(subPath.getFileName().toString());
                String contents;
                try (InputStream stream = testClass.getClassLoader().getResourceAsStream(resourceName)) {
                    contents = CharStreams.toString(new InputStreamReader(stream, UTF_8));
                }
                switch (extension) {
                    case "input":
                        inputs.put(baseName, contents);
                        break;
                    case "output":
                        outputs.put(baseName, contents);
                        break;
                    default:
                }
            }
        }
        List<Object[]> testInputs = new ArrayList<>();
        if (!isRecreate()) {
            assertThat(outputs).describedAs("unmatched inputs and outputs").hasSameSizeAs(inputs);
        }
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            String fileName = entry.getKey();
            String input = inputs.get(fileName);

            String expectedOutput;
            if (isRecreate()) {
                expectedOutput = null;
            } else {
                assertThat(outputs).describedAs("unmatched input").containsKey(fileName);
                expectedOutput = outputs.get(fileName);
            }
            testInputs.add(new Object[] {fileName, input, expectedOutput});
        }
        return testInputs;
    }

    public static boolean isRecreate() {
        return Boolean.getBoolean("recreate");
    }

    private Path getOutputTestPath(String testName) {
        return fullTestPath.resolve(testName + ".output");
    }

    public void writeFormatterOutput(String testName, String output) {
        try (BufferedWriter writer = Files.newBufferedWriter(getOutputTestPath(testName))) {
            writer.append(output);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't recreate test output for " + testName, e);
        }
    }
}
