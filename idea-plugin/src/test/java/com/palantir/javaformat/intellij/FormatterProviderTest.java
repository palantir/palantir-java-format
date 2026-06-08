/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.javaformat.intellij;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class FormatterProviderTest {

    @TempDir
    Path gradleHome;

    @TempDir
    Path outsideDir;

    @Test
    void testParseSdkJavaVersion_major() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15")).hasValue(15);
    }

    @Test
    void testParseSdkJavaVersion_majorMinorPatch() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15.0.2")).hasValue(15);
    }

    @Test
    void testParseSdkJavaVersion_ea() {
        assertThat(FormatterProvider.parseSdkJavaVersion("15-ea")).hasValue(15);
    }

    @Test
    void testParseSdkJavaVersion_invalidVersion_isEmpty() {
        assertThat(FormatterProvider.parseSdkJavaVersion("not-a-version")).isEmpty();
    }

    @Test
    void resolveGradleUserHome_returnsNonNullNormalizedPath() {
        Path home = FormatterProvider.resolveGradleUserHome();
        assertThat(home).isAbsolute();
        assertThat(home.toString()).doesNotContain("..");
    }

    @Test
    void validateClasspathEntry_acceptsPathInsideGradleUserHome() throws IOException {
        Path jar = Files.createFile(gradleHome.resolve("some.jar"));
        FormatterProvider.validateClasspathEntry(jar.toUri(), gradleHome);
    }

    @Test
    void validateClasspathEntry_acceptsNestedPathInsideGradleUserHome() throws IOException {
        Path caches = Files.createDirectories(gradleHome.resolve("caches/modules-2"));
        Path jar = Files.createFile(caches.resolve("some.jar"));
        FormatterProvider.validateClasspathEntry(jar.toUri(), gradleHome);
    }

    @Test
    void validateClasspathEntry_rejectsPathOutsideGradleUserHome() throws IOException {
        Path externalJar = Files.createFile(outsideDir.resolve("external.jar"));
        assertThatThrownBy(() -> FormatterProvider.validateClasspathEntry(externalJar.toUri(), gradleHome))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not within the Gradle user home");
    }

    @Test
    void validateClasspathEntry_rejectsNonExistentPath() {
        URI missing = gradleHome.resolve("does-not-exist.jar").toUri();
        assertThatThrownBy(() -> FormatterProvider.validateClasspathEntry(missing, gradleHome))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to resolve real path");
    }

    @Test
    void validateClasspathEntry_rejectsSymlinkPointingOutsideGradleHome() throws IOException {
        Path externalJar = Files.createFile(outsideDir.resolve("external.jar"));
        Path symlink = gradleHome.resolve("linked.jar");
        Files.createSymbolicLink(symlink, externalJar);

        assertThatThrownBy(() -> FormatterProvider.validateClasspathEntry(symlink.toUri(), gradleHome))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not within the Gradle user home");
    }

    @Test
    void validateClasspathEntry_rejectsNonFileScheme() {
        URI httpUri = URI.create("http://example.com/external.jar");
        assertThatThrownBy(() -> FormatterProvider.validateClasspathEntry(httpUri, gradleHome))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use the 'file' scheme");
    }

    @Test
    void validateClasspathEntry_rejectsUriWithNoScheme() {
        URI noSchemeUri = URI.create("some-path/external.jar");
        assertThatThrownBy(() -> FormatterProvider.validateClasspathEntry(noSchemeUri, gradleHome))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use the 'file' scheme");
    }
}
