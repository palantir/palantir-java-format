/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.io.File;
import org.junit.jupiter.api.Test;

/**
 * When we were getting gradle-baseline to support the configuration cache, spotless had some poorly written tasks
 * which caused issues with the configuration cache.
 * <p>
 * Bumping spotless to 6.22.0 or newer fixed this, but revealed a new error — the {@code palantirJavaFormat} configuration was
 * being <a href="https://github.com/palantir/palantir-java-format/blob/b7b5995df3be690780939c0d0cb2ec49b99c68c8/gradle-palantir-java-format/src/main/java/com/palantir/javaformat/gradle/spotless/NativePalantirJavaFormatStep.java#L45"> resolved eagerly</a>.
 * <p>
 * gradle-consistent-versions enforces against resolving configurations at configuration time, and throws an error.
 * <p>
 * This test forces creation of the spotless steps, which will reveal any eager resolution of configurations.
 */
@GradlePluginTests
class SupportsCurrentSpotlessTest {
    private static final String CLASSPATH_FILE = new File("build/impl.classpath").getAbsolutePath();

    @Test
    void palantirjavaformatplugin_works_with_current_spotless(GradleInvoker gradle, RootProject rootProject) {
        rootProject
                .buildGradle()
                .plugins()
                .add("java")
                .add("com.palantir.java-format")
                .add("com.palantir.consistent-versions")
                .add("com.diffplug.spotless");

        rootProject.file("versions.props").createEmpty();
        rootProject.file("versions.lock").createEmpty();

        gradle.withArgs("wrapper").buildsSuccessfully();

        rootProject.buildGradle().append("""
            dependencies {
                palantirJavaFormat files(file("%s").text.split(':'))
            }

            // This forces the realization of the spotlessJava task, creating the spotless steps.
            // If any configurations are eagerly resolved in the spotless steps,
            // consistent-versions should catch it and throw here.
            project.getTasks().getByName("spotlessJava")
            """, CLASSPATH_FILE);

        gradle.withArgs("classes", "--info").buildsSuccessfully();
    }
}
