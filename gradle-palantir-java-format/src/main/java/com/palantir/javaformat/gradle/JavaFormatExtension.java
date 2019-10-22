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

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class JavaFormatExtension {
    private final Property<String> implementationVersion;
    private static final String IMPLEMENTATION_VERSION =
            JavaFormatExtension.class.getPackage().getImplementationVersion();

    public JavaFormatExtension(Project project) {
        implementationVersion = project.getObjects().property(String.class);
        implementationVersion.set(IMPLEMENTATION_VERSION);
    }

    public final Property<String> getImplementationVersion() {
        return implementationVersion;
    }
}
