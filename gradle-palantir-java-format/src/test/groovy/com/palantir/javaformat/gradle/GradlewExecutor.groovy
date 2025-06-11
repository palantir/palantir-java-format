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

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream

class GradlewExecutor {
    File projectDir

    GradlewExecutor(File projectDir) {
        this.projectDir = projectDir
    }

    private static Iterable<File> getBuildPluginClasspathInjector() {
        return getPluginClasspathInjector(Path.of("../gradle-palantir-java-format/build/pluginUnderTestMetadata/plugin-under-test-metadata.properties"))
    }

    private static Iterable<File> getPluginClasspathInjector(Path path) {
        File propertiesFile = path.toFile()
        Properties properties = new Properties()
        propertiesFile.withInputStream { inputStream ->
            properties.load(inputStream)
        }
        String classpath = properties.getProperty('implementation-classpath')
        return classpath.split(File.pathSeparator).collect { new File(it) }
    }

    GradlewExecutionResult runGradlewTasks(String... tasks) {
        ProcessBuilder processBuilder = getProcessBuilder(tasks)
        Process process = processBuilder.start()
        String output = readAllInput(process.getInputStream())
        process.waitFor(1, TimeUnit.MINUTES)
        GradlewExecutionResult result = new GradlewExecutionResult(process.exitValue(), output)
        return result
    }

    final class GradlewExecutionResult {

        final Boolean success
        final String standardOutput
        final Throwable failure

        GradlewExecutionResult(int exitValue, String output) {
            this.success = exitValue == 0
            this.standardOutput =  output
            this.failure = exitValue != 0 ? new RuntimeException(String.format("Build failed with exitCode %s", exitValue)) : null
        }
    }

    static String readAllInput(InputStream inputStream) {
        try (Stream<String> lines =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    private ProcessBuilder getProcessBuilder(String... tasks) {
        File initScript = new File(projectDir, "init.gradle")
        ClasspathAddingInitScriptBuilder.build(initScript, getBuildPluginClasspathInjector().toList())
        List<String> arguments = ["./gradlew", "--init-script", initScript.toPath().toString()]
        Arrays.asList(tasks).forEach(arguments::add)
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(arguments)
                .directory(projectDir)
                .redirectErrorStream(true)
        return processBuilder
    }

}
