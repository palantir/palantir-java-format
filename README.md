<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/palantir-java-format"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

# palantir-java-format

_A modern, lambda-friendly, 120 character Java formatter._

- [IntelliJ plugin](https://plugins.jetbrains.com/plugin/13180-palantir-java-format)
- [Gradle plugin](https://github.com/palantir/gradle-baseline#compalantirbaseline-format)
- [Maven plugin via Spotless](#maven-integration-via-spotless)

It is based on the excellent [google-java-format](https://github.com/google/google-java-format), and benefits from the work of all the [original authors](https://github.com/google/google-java-format/graphs/contributors). palantir-java-format is available under the same [Apache 2.0 License](./LICENSE).

## Upsides of automatic formatting

- reduce 'nit' comments in code reviews, allowing engineers to focus on the important logic rather than bikeshedding about whitespace
- bot-authored code changes can be auto-formatted to a highly readable style (we use [refaster](https://errorprone.info/docs/refaster) and [error-prone](https://errorprone.info/docs/patching) heavily)
- increased consistency across all repos, so contributing to other projects feels familiar
- reduce the number builds that trivially fail checkstyle
- easier to onboard new devs

## Downsides of automatic formatting

- if you don't like how the formatter laid out your code, you may need to introduce new functions/variables
- the formatter is not as clever as humans are, so it can sometimes produce less readable code (we want to fix this where feasible)

Many other languages have already adopted formatters enthusiastically, including typescript (prettier), go (gofmt), rust (rustfmt).

## Motivation & examples

(1) before:

```java
private static void configureResolvedVersionsWithVersionMapping(Project project) {
    project.getPluginManager()
            .withPlugin(
                    "maven-publish",
                    plugin -> {
                        project.getExtensions()
                                .getByType(PublishingExtension.class)
                                .getPublications()
                                .withType(MavenPublication.class)
                                .configureEach(
                                        publication ->
                                                publication.versionMapping(
                                                        mapping -> {
                                                            mapping.allVariants(
                                                                    VariantVersionMappingStrategy
                                                                            ::fromResolutionResult);
                                                        }));
                    });
}
```

(1) after:

```java
private static void configureResolvedVersionsWithVersionMapping(Project project) {
    project.getPluginManager().withPlugin("maven-publish", plugin -> {
        project.getExtensions()
                .getByType(PublishingExtension.class)
                .getPublications()
                .withType(MavenPublication.class)
                .configureEach(publication -> publication.versionMapping(mapping -> {
                    mapping.allVariants(VariantVersionMappingStrategy::fromResolutionResult);
                }));
    });
}
```

(2) before:

```java
private static GradleException notFound(
        String group, String name, Configuration configuration) {
    String actual =
            configuration.getIncoming().getResolutionResult().getAllComponents().stream()
                    .map(ResolvedComponentResult::getModuleVersion)
                    .map(
                            mvi ->
                                    String.format(
                                            "\t- %s:%s:%s",
                                            mvi.getGroup(), mvi.getName(), mvi.getVersion()))
                    .collect(Collectors.joining("\n"));
    // ...
}
```

(2) after:

```java
private static GradleException notFound(String group, String name, Configuration configuration) {
    String actual = configuration.getIncoming().getResolutionResult().getAllComponents().stream()
            .map(ResolvedComponentResult::getModuleVersion)
            .map(mvi -> String.format("\t- %s:%s:%s", mvi.getGroup(), mvi.getName(), mvi.getVersion()))
            .collect(Collectors.joining("\n"));
    // ...
}
```

## Optimised for code review

Even though PJF sometimes inlines code more than other formatters, reducing what we see as unnecessary breaks that don't help code comprehension, there are also cases where it will split code into more lines too, in order to improve clarity and code reviewability.

One such case is long method chains. Whereas other formatters are content to completely one-line a long method call chain if it fits, it doesn't usually produce a very readable result:

```java
var foo = SomeType.builder().thing1(thing1).thing2(thing2).thing3(thing3).build();
```

To avoid this edge case, we employ a limit of 80 chars for chained method calls, such that _the last method call dot_ must come before that column, or else the chain is not inlined.

```java
var foo = SomeType.builder()
        .thing1(thing1)
        .thing2(thing2)
        .thing3(thing3)
        .build();
```

## com.palantir.java-format gradle plugin

You should apply this plugin to all projects where you want your java code formatted, e.g.

```groovy
buildscript {
    dependencies {
        classpath 'com.palantir.javaformat:gradle-palantir-java-format:<version>'
    }
}
allprojects {
    apply plugin: 'com.palantir.java-format'
}
```

## Maven Integration via Spotless

palantir-java-format is supported by [spotless](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md#palantir-java-format)
out of the box.

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>${spotless.version}</version>
    <configuration>
      <java>
        <palantirJavaFormat>
            <!-- Optionally specify a version -->
            <version>${palantirJavaFormat.version}</version>
        </palantirJavaFormat>
      </java>
    </configuration>
</plugin>
```

Applying this automatically configures IntelliJ, whether you run `./gradlew idea`
or import the project directly from IntelliJ, to use the correct version of the formatter
when formatting java code.

If using [com.palantir.baseline-idea](https://plugins.gradle.org/plugin/com.palantir.baseline-idea)
in conjunction with this plugin, the Save Actions IntelliJ plugin will be recommended
and automatically configured to auto-format all your java source files on save.

## IntelliJ plugin

A
[palantir-java-format IntelliJ plugin](https://plugins.jetbrains.com/plugin/13180)
is available from the plugin repository. To install it, go to your IDE's
settings and select the `Plugins` category. Click the `Marketplace` tab, search
for the `palantir-java-format` plugin, and click the `Install` button.

The plugin will be disabled by default on new projects, but as mentioned [above](#compalantirjava-format-gradle-plugin), 
if using the `com.palantir.java-format` gradle plugin, it will be recommended 
in IntelliJ, and automatically configured.

To manually enable it in the current project, go
to `File→Settings...→palantir-java-format Settings` (or `IntelliJ
IDEA→Preferences...→Other Settings→palantir-java-format Settings` on macOS) and
check the `Enable palantir-java-format` checkbox.

To enable it by default in new projects, use `File→Other Settings→Default
Settings...`.

When enabled, it will replace the normal `Reformat Code` action, which can be
triggered from the `Code` menu or with the Ctrl-Alt-L (by default) keyboard
shortcut.

## Future works

- [ ] preserve [NON-NLS markers][] - these are comments that are used when implementing NLS internationalisation, and need to stay on the same line with the strings they come after.

[NON-NLS markers]: https://stackoverflow.com/a/40266605

## License

```text
(c) Copyright 2019 Palantir Technologies Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```

This is a fork of [google-java-format](https://github.com/google/google-java-format).
Original work copyrighted by Google under the same license:

```text
Copyright 2015 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
