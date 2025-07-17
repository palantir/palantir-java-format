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

package com.palantir.javaformat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Measurement(iterations = 2, time = 2)
@Warmup(iterations = 2, time = 4)
public class BenchmarkMultiFiles {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        final List<String> filesToFormat = getFilesToFormat();

        private static List<String> getFilesToFormat() {
            Path srcJavaFormatFiles = Paths.get(".")
                    .toAbsolutePath()
                    .resolve("../palantir-java-format/src/main/java/com/palantir/javaformat/java");
            try (Stream<String> paths = Files.list(srcJavaFormatFiles)
                    .filter(Files::isRegularFile)
                    .map(path -> path.toAbsolutePath().toString())) {
                return paths.collect(Collectors.toList());
            } catch (IOException e) {
                throw new UncheckedIOException("Couldn't list src files: " + srcJavaFormatFiles, e);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public final void runNativeImage(BenchmarkState state) throws InterruptedException, IOException {
        ProcessBuilder p = new ProcessBuilder();
        p.command(Stream.concat(
                        Stream.of(
                                Path.of(System.getenv("NATIVE_IMAGE_CLASSPATH")).toString(), "-i", "--palantir"),
                        state.filesToFormat.stream())
                .collect(Collectors.toList()));
        Process process = p.inheritIO().start();
        assertThat(process.waitFor()).isEqualTo(0);
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public final void runJar(BenchmarkState state) throws InterruptedException, IOException {
        ProcessBuilder p = new ProcessBuilder();
        p.command(Stream.concat(
                        Stream.of(
                                "java",
                                "-cp",
                                System.getenv("JARS_CLASSPATH"),
                                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                                "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                                "com.palantir.javaformat.java.Main",
                                "-i",
                                "--palantir"),
                        state.filesToFormat.stream())
                .collect(Collectors.toList()));
        Process process = p.inheritIO().start();
        assertThat(process.waitFor()).isEqualTo(0);
    }

    public static void main(String[] _args) throws RunnerException {
        new Runner(new OptionsBuilder()
                        .include(BenchmarkMultiFiles.class.getSimpleName())
                        .build())
                .run();
    }
}
