/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

final class FormatterCommandRunner {
    private static final Pattern SYNTAX_ERROR_PATTERN = Pattern.compile(":\\d+:\\d+:\\serror:\\s");

    static Optional<String> runWithStdin(List<String> command, String input) throws IOException {
        Process process = new ProcessBuilder().command(command).start();

        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.write(input.getBytes(StandardCharsets.UTF_8));
        }

        // Make sure to drain stdout before waiting for the process to exit as this can result in a deadlock otherwise.
        String stdout = readToString(process.getInputStream());

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while executing command", e);
        }

        if (process.exitValue() != 0) {
            String stderr = readToString(process.getErrorStream());
            if (isSyntaxError(stderr)) {
                // Don't surface errors due to the formatter failing to parse the java file due to syntax errors.
                // In this case, we just want to silently do nothing and not surface an error to e.g. Intellij.
                return Optional.empty();
            }
            throw new IOException(getErrorMessage(command, stdout, stderr));
        }

        return Optional.of(stdout);
    }

    /**
     * Determine if the command failed due to a syntax error. These errors have the format of:
     * "<stdin>:47:13: error: class, interface, enum, or record expected"
     */
    private static boolean isSyntaxError(String stderr) {
        return SYNTAX_ERROR_PATTERN.matcher(stderr).find();
    }

    private static String readToString(InputStream input) throws IOException {
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String getErrorMessage(List<String> command, String stdout, String stderr) {
        return String.join(
                "\n",
                "Command terminated with exit value 1",
                "Command: " + String.join(" ", command),
                "Stdout:",
                stdout,
                "Stderr:",
                stderr);
    }
}
