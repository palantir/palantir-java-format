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

package com.palantir.javaformat.gradle

import nebula.test.functional.internal.classpath.ClasspathAddingInitScriptBuilder
import org.gradle.internal.impldep.org.eclipse.jgit.annotations.NonNull
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * {@link IntegrationTestKitSpec} currently loads <a href="https://github.com/nebula-plugins/nebula-test/blob/c5d3af9004898276bde5c68da492c6b0b4c5facc/src/main/groovy/nebula/test/IntegrationTestKitBase.groovy#L136"> more than what it needs into the classpath</a>.
 * This means if we run a test with {@link IntegrationTestKitSpec}'s runner, the {@link Formatter} is on the build's classpath by virtue of being in the test's classpath.
 * If the test applies the {@link PalantirJavaFormatPlugin}, it complains that the {@link Formatter} is <a href="https://github.com/palantir/palantir-java-format/blob/00b08d2f471d66382d6c4cd2d05f56b6bb546ad3/gradle-palantir-java-format/src/main/java/com/palantir/javaformat/gradle/spotless/PalantirJavaFormatStep.java#L83">erroneously loadable</a>.
 * To be clear, this complaint is entirely a result of the {@link IntegrationTestKitSpec} loading too many things onto classpath since it doesn't know what the exact plugin classpath is.
 * As a workaround, this runner uses the classpath produced by Gradle Test Kit in {@code plugin-under-test-metadata.properties}.
 * This classpath only contains the dependencies required by the plugin, as well as the plugin itself.
 * This means that even if we put the formatter on the {@code testClassPath}, it won't leak through to the Gradle build under test and subsequently no error from {@link PalantirJavaFormatStep}.
 */
class GradlewExecutor {
    private File projectDir

    GradlewExecutor(@NonNull File projectDir) {
        this.projectDir = projectDir
    }

    private static List<File> getBuildPluginClasspathInjector() {
        return PluginUnderTestMetadataReading.readImplementationClasspath();
    }

    GradlewExecutionResult runGradlewTasks(String... tasks) {
        ProcessBuilder processBuilder = getProcessBuilder(tasks)
        Process process = processBuilder.start()
        String output = readAllInput(process.getInputStream())
        process.waitFor(1, TimeUnit.MINUTES)
        return new GradlewExecutionResult(process.exitValue(), output)
    }

    final class GradlewExecutionResult {
        final boolean success
        final String standardOutput
        final Throwable failure

        GradlewExecutionResult(int exitValue, String output) {
            this.success = exitValue == 0
            this.standardOutput =  output
            this.failure = exitValue != 0 ? new RuntimeException(String.format("Build failed with exitCode %s", exitValue)) : null
        }
    }

    private static String readAllInput(InputStream inputStream) {
        try (Stream<String> lines =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    private ProcessBuilder getProcessBuilder(String... tasks) {
        File initScript = new File(projectDir, "init.gradle")
        ClasspathAddingInitScriptBuilder.build(initScript, getBuildPluginClasspathInjector())
        List<String> arguments = ["./gradlew", "--init-script", initScript.toPath().toString()]
        Arrays.asList(tasks).forEach(arguments::add)
        return new ProcessBuilder()
                .command(arguments)
                .directory(projectDir)
                .redirectErrorStream(true)
    }
}
