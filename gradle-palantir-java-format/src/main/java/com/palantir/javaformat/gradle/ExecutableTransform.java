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

package com.palantir.javaformat.gradle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;

/**
 * A transform that makes the input artifact executable.
 *
 * <p>This is useful for native-image executables, which need to be executable in order to run.
 */
public abstract class ExecutableTransform implements TransformAction<TransformParameters.None> {

    private static Logger logger = Logging.getLogger(ExecutableTransform.class);

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File inputFile = getInputArtifact().get().getAsFile();
        File outputFile = outputs.file(inputFile.getName() + ".executable");
        try {
            Files.copy(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            makeFileExecutable(outputFile.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to create executable file %s", outputFile.toPath()), e);
        }
    }

    private static void makeFileExecutable(Path pathToExe) {
        try {
            Set<PosixFilePermission> existingPermissions = Files.getPosixFilePermissions(pathToExe);
            Files.setPosixFilePermissions(
                    pathToExe,
                    Stream.concat(
                                    existingPermissions.stream(),
                                    Stream.of(
                                            PosixFilePermission.OWNER_EXECUTE,
                                            PosixFilePermission.GROUP_EXECUTE,
                                            PosixFilePermission.OTHERS_EXECUTE))
                            .collect(Collectors.toSet()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to set execute permissions on native-image", e);
        }
    }
}
