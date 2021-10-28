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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class ProcessRunner {

    static String runWithStdin(List<String> command, String input) throws IOException {
        System.out.println(">>> CMD: " + String.join(" ", command));

        Process process = new ProcessBuilder().command(command).start();

        BufferedWriter processOutputStream =
                new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        processOutputStream.write(input);
        processOutputStream.close();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while executing command", e);
        }

        String stdout = readToString(process.getInputStream());

        if (process.exitValue() != 0) {
            String stderr = readToString(process.getErrorStream());
            throw new IOException(String.format(
                    "Command terminated with exit code 1.\nStdout: \"%s\",\nStderr: \"%s\"", stdout, stderr));
        }

        return stdout;
    }

    private static String readToString(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            return builder.toString();
        }
    }
}
