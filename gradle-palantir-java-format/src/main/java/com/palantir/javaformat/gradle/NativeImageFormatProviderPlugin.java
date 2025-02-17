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

import com.google.common.base.Preconditions;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public final class NativeImageFormatProviderPlugin implements Plugin<Project> {

    private static final Logger log = Logging.getLogger(NativeImageFormatProviderPlugin.class);
    static final String NATIVE_CONFIGURATION_NAME = "palantirJavaFormatNative";

    @Override
    public void apply(Project rootProject) {
        Preconditions.checkState(
                rootProject == rootProject.getRootProject(),
                "May only apply com.palantir.java-format-provider to the root project");

        if (isNativeImageSupported()) {
            log.info("Skipping native image configuration as it is not supported on this platform");
            return;
        }
        String implementationVersion = JavaFormatExtension.class.getPackage().getImplementationVersion();
        rootProject.getConfigurations().register(NATIVE_CONFIGURATION_NAME, conf -> {
            conf.setDescription("Internal configuration for resolving the palantir-java-format native image");
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);
            conf.defaultDependencies(deps -> {
                deps.add(rootProject
                        .getDependencies()
                        .create(String.format(
                                "com.palantir.javaformat:palantir-java-format-native:%s:nativeImage-%s_%s@%s",
                                implementationVersion, getCurrentArch(), getCurrentOs(), getExtension())));
            });
            conf.getAttributes().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        });
        rootProject.getDependencies().registerTransform(ExecutableTransform.class, transformSpec -> {
            transformSpec.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, getExtension());
            transformSpec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "executable-nativeImage");
        });
    }

    public static boolean isNativeImageSupported() {
        // TODO(crogoz): check if musl or glibc
        if (getCurrentOs().equals("linux")) {
            return true;
        }
        if (getCurrentOs().equals("macos") && getCurrentArch().equals("aarh64")) {
            return true;
        }
        log.info("Not using the native image for the current OS and Arch");
        return false;
    }

    public static boolean shouldUseNativeImage(Project project) {
        return Optional.ofNullable(project.findProperty("palantir.native.formatter"))
                .map(value -> Boolean.getBoolean((String) value))
                .orElse(false);
    }

    static String getExtension() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("windows")) {
            return "exe";
        }
        return "bin";
    }

    static String getCurrentOs() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("windows")) {
            return "windows";
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            return "macos";
        } else if (osName.contains("linux")) {
            return "linux";
        }
        throw new GradleException(String.format("Invalid Operating System %s", osName));
    }

    static String getCurrentArch() {
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (Set.of("x86_64", "x64", "amd64").contains(osArch)) {
            return "x86_64";
        }
        if (Set.of("arm", "arm64", "aarch64").contains(osArch)) {
            return "aarch64";
        }
        if (Set.of("x86", "i686").contains(osArch)) {
            return "x86";
        }
        throw new GradleException(String.format("Invalid Operating System %s", osArch));
    }
}
