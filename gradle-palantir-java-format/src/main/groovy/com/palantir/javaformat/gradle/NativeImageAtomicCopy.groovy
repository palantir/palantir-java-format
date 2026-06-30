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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class NativeImageAtomicCopy {

    private static Logger logger = Logging.getLogger(PalantirJavaFormatIdeaPlugin.class);

    public static URI copyToCacheDir(Path src, Path dst) {
        if (Files.exists(dst)) {
            logger.info("Native image at path {} already exists", dst);
            return dst.toUri();
        }
        Path lockFile = dst.getParent().resolve(dst.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(
                lockFile, StandardOpenOption.READ, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            channel.lock();
            // double-check, now that we hold the lock
            if (Files.exists(dst)) {
                logger.info("Native image at path {} already exists", dst);
                return dst.toUri();
            }
            try {
                // Attempt an atomic move first to avoid broken partial states.
                // Failing that, we use the replace_existing option such that
                // the results of a successful move operation are consistent.
                // This provides a helpful property in a race where the slower
                // process doesn't risk attempting to use the native image before
                // it has been fully moved.
                Path tempCopyDir = Files.createTempDirectory("tempDir");
                Path tmpFile = tempCopyDir.resolve(src.getFileName());
                try {
                    Files.copy(src, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tmpFile, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(tmpFile, dst, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    FileUtils.delete(tempCopyDir);
                }
            } catch (FileAlreadyExistsException e) {
                // This means another process has successfully installed this native image, and we can just use theirs.
                // Should be unreachable using REPLACE_EXISTING, however kept around to prevent issues with potential
                // future refactors.
            }
            return dst.toUri();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy the native image to path %s", dst), e);
        }
    }

    private NativeImageAtomicCopy() {}
}
