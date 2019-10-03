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

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.palantir.javaformat.Newlines;
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
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Integration test for google-java-format. */
@RunWith(Parameterized.class)
public class FormatterIntegrationTest {

  private static final Path TEST_DATA_PATH = Paths.get("com/google/googlejavaformat/java/testdata");
  /** Where to output test outputs when recreating. */
  private static final Path OUTPUT_TEST_PATH =
      Paths.get("src/test/resources").resolve(TEST_DATA_PATH);

  @Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> data() throws IOException {
    ClassLoader classLoader = FormatterIntegrationTest.class.getClassLoader();
    Map<String, String> inputs = new TreeMap<>();
    Map<String, String> outputs = new TreeMap<>();
    for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
      String resourceName = resourceInfo.getResourceName();
      Path resourceNamePath = Paths.get(resourceName);
      if (resourceNamePath.startsWith(TEST_DATA_PATH)) {
        Path subPath = TEST_DATA_PATH.relativize(resourceNamePath);
        assertEquals("bad testdata file names", 1, subPath.getNameCount());
        String baseName = getNameWithoutExtension(subPath.getFileName().toString());
        String extension = getFileExtension(subPath.getFileName().toString());
        String contents;
        try (InputStream stream =
            FormatterIntegrationTest.class.getClassLoader().getResourceAsStream(resourceName)) {
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
    return testInputs;
  }

  private final String name;
  private final String input;
  private final String expected;
  private final String separator;

  public FormatterIntegrationTest(String name, String input, String expected) {
    this.name = name;
    this.input = input;
    this.expected = expected;
    this.separator = isRecreate() ? null : Newlines.getLineEnding(expected);
  }

  @Test
  public void format() {
    try {
      String output = createFormatter().formatSource(input);
      if (isRecreate()) {
        writeFormatterOutput(output);
        return;
      }
      assertEquals("bad output for " + name, expected, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  private static Formatter createFormatter() {
    return new Formatter(
        JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());
  }

  @Test
  public void idempotentLF() {
    Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
    try {
      String mangled = expected.replace(separator, "\n");
      String output = createFormatter().formatSource(mangled);
      assertEquals("bad output for " + name, mangled, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  @Test
  public void idempotentCR() throws IOException {
    Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
    try {
      String mangled = expected.replace(separator, "\r");
      String output = createFormatter().formatSource(mangled);
      assertEquals("bad output for " + name, mangled, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  @Test
  public void idempotentCRLF() {
    Assume.assumeFalse("Not running when recreating test outputs", isRecreate());
    try {
      String mangled = expected.replace(separator, "\r\n");
      String output = createFormatter().formatSource(mangled);
      assertEquals("bad output for " + name, mangled, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  private static boolean isRecreate() {
    return Boolean.getBoolean("recreate");
  }

  private Path getOutputTestPath() {
    return OUTPUT_TEST_PATH.resolve(name + ".output");
  }

  private void writeFormatterOutput(String output) {
    try (BufferedWriter writer = Files.newBufferedWriter(getOutputTestPath())) {
      writer.append(output);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
