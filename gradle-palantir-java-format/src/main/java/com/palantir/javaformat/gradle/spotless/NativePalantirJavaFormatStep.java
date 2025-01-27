/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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
import com.diffplug.spotless.ProcessRunner;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class NativePalantirJavaFormatStep {
    private static Logger logger = Logging.getLogger(NativePalantirJavaFormatStep.class);

    private NativePalantirJavaFormatStep() {}

    private static final String NAME = "palantir-java-format";

    /** Creates a step which formats everything - code, import order, and unused imports. */
    public static FormatterStep create(Configuration configuration) {
        return FormatterStep.createLazy(
                NAME,
                () -> {
                    logger.info("files {}", configuration.getSingleFile());
                    return new State(FileSignature.signAsSet(configuration.getSingleFile()));
                },
                State::createFormat);
    }

    static class State implements Serializable {
        private static final long serialVersionUID = 1L;

        final FileSignature pathToExe;

        State(FileSignature pathToExe) {
            this.pathToExe = pathToExe;
            try {
                Set<PosixFilePermission> existingPermissions =
                        Files.getPosixFilePermissions(pathToExe.getOnlyFile().toPath());
                Files.setPosixFilePermissions(
                        pathToExe.getOnlyFile().toPath(),
                        Stream.concat(
                                        existingPermissions.stream(),
                                        Stream.of(
                                                PosixFilePermission.OWNER_EXECUTE,
                                                PosixFilePermission.GROUP_EXECUTE,
                                                PosixFilePermission.OTHERS_EXECUTE))
                                .collect(Collectors.toSet()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to set execute permissions", e);
            }
        }

        String format(ProcessRunner runner, String input) throws IOException, InterruptedException {
            List<String> argumentsWithPathToExe =
                    List.of(pathToExe.getOnlyFile().getAbsolutePath(), "--palantir", "-");
            return runner.exec(input.getBytes(StandardCharsets.UTF_8), argumentsWithPathToExe)
                    .assertExitZero(StandardCharsets.UTF_8);
        }

        FormatterFunc.Closeable createFormat() {
            ProcessRunner runner = new ProcessRunner();
            return FormatterFunc.Closeable.of(runner, this::format);
        }
    }
}
