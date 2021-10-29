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

final class ProcessRunner {

    static String runWithStdin(List<String> command, String input) throws IOException {
        Process process = new ProcessBuilder().command(command).start();

        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.write(input.getBytes(StandardCharsets.UTF_8));
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while executing command", e);
        }

        String stdout = readToString(process.getInputStream());

        if (process.exitValue() != 0) {
            String stderr = readToString(process.getErrorStream());
            throw new IOException(String.format(
                    "Command terminated with exit code 1.\n" + "Stdout: \"%s\"\n" + "Stderr: \"%s\"\n" + "Command: %s",
                    stdout, stderr, String.join(" ", command)));
        }

        return stdout;
    }

    private static String readToString(InputStream input) throws IOException {
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
