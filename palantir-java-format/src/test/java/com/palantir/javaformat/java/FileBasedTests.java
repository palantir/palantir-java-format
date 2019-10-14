package com.palantir.javaformat.java;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

public final class FileBasedTests {

    private final Class<?> testClass;
    /** The path prefix for all tests if loaded as resources. */
    private final Path resourcePrefix;
    /** Where to output test outputs when recreating. */
    private final Path fullTestPath;

    public FileBasedTests(Class<?> testClass) {
        this(testClass, testClass.getSimpleName());
    }

    public FileBasedTests(Class<?> testClass, String testDirName) {
        this.resourcePrefix = Paths.get(testClass.getPackage().getName().replace('.', '/')).resolve(testDirName);
        this.testClass = testClass;
        this.fullTestPath = Paths.get("src/test/resources").resolve(resourcePrefix);
    }

    public Object[][] paramsAsNameInputOutput() throws IOException {
        ClassLoader classLoader = testClass.getClassLoader();
        Map<String, String> inputs = new TreeMap<>();
        Map<String, String> outputs = new TreeMap<>();
        for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
            String resourceName = resourceInfo.getResourceName();
            Path resourceNamePath = Paths.get(resourceName);
            if (resourceNamePath.startsWith(resourcePrefix)) {
                Path subPath = resourcePrefix.relativize(resourceNamePath);
                assertEquals("bad testdata file names", 1, subPath.getNameCount());
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
            assertEquals("unmatched inputs and outputs", inputs.size(), outputs.size());
        }
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            String fileName = entry.getKey();
            String input = inputs.get(fileName);

            String expectedOutput;
            if (isRecreate()) {
                expectedOutput = null;
            } else {
                assertTrue("unmatched input", outputs.containsKey(fileName));
                expectedOutput = outputs.get(fileName);
            }
            testInputs.add(new Object[] {fileName, input, expectedOutput});
        }
        return testInputs.toArray(new Object[][] {});
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
