<p align=right>
<a href=https://autorelease.bots.palantir.build/palantir/palantir-java-format><img src=https://shields.palantir.build/badge/Perform%20an-Autorelease-brightgreen.svg alt=Autorelease></a>
</p>

# palantir-java-format

`palantir-java-format` is a program that reformats Java source code to comply with
[Palantir Java Style][].

It is a fork of [google-java-format], but intending to achieve the following goals:

* Enforce the Palantir Java Style, whose main difference compared to Google's style is that we use 120 character lines and 4 space indent.

* Produce more compact code - Keep long method chains and invocations, as well as assignments and lambdas, on a single line, if we can break only the last argument of a method call, or the last method call in a chain.

* don't break [NON-NLS markers][] - these are comments that are used when implementing NLS internationalisation, and need to stay on the same line with the strings they come after.

We thank everyone who's worked on google-java-format for their good work and for making it available for free online. It's thanks to their work that we could incorporate our coding style preferences into a formatter so easily.

[google-java-format]: https://github.com/google/google-java-format 
[Palantir Java Style]: https://github.com/palantir/gradle-baseline/blob/develop/docs/java-style-guide/readme.md
[NON-NLS markers]: https://stackoverflow.com/a/40266605

## Using the formatter

### from the command-line

[Download the formatter](https://github.com/palantir/palantir-java-format/releases)
and run it with:

```
java -jar /path/to/palantir-java-format-<version>-all-deps.jar <options> [files...]
```

The formatter can act on whole files, on limited lines (`--lines`), on specific
offsets (`--offset`), passing through to standard-out (default) or altered
in-place (`--replace`).

***Note:*** *There is no configurability as to the formatter's algorithm for
formatting. This is a deliberate design decision to unify our code formatting on
a single format.*

### IntelliJ, Android Studio, and other JetBrains IDEs

A
[palantir-java-format IntelliJ plugin](https://plugins.jetbrains.com/plugin/8527)
is available from the plugin repository. To install it, go to your IDE's
settings and select the `Plugins` category. Click the `Marketplace` tab, search
for the `palantir-java-format` plugin, and click the `Install` button.

The plugin will be enabled by default. To disable it in the current project, go
to `File→Settings...→palantir-java-format Settings` (or `IntelliJ
IDEA→Preferences...→Other Settings→palantir-java-format Settings` on macOS) and
check the `Enable palantir-java-format` checkbox. (A notification will be
presented when you first open a project offering to do this for you.)

To disable it by default in new projects, use `File→Other Settings→Default
Settings...`.

When enabled, it will replace the normal `Reformat Code` action, which can be
triggered from the `Code` menu or with the Ctrl-Alt-L (by default) keyboard
shortcut.

The import ordering is not handled by this plugin, unfortunately. To fix the
import order, download the
[IntelliJ Java Google Style file](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
and import it into File→Settings→Editor→Code Style.

### Eclipse

A palantir-java-format Eclipse plugin is not currently published but can be built individually. 
Once built, drop it into the Eclipse
[drop-ins folder](http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Fp2_dropins_format.html)
to activate the plugin.

The plugin adds a `palantir-java-format` formatter implementation that can be
configured in `Window > Preferences > Java > Code Style > Formatter > Formatter
Implementation`.

### Third-party integrations

*   Gradle plugins
    *   [Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-to-java-source-google-java-format):
    *   [sherter/google-java-format-gradle-plugin](https://github.com/sherter/google-java-format-gradle-plugin)
*   Apache Maven plugins
    *   [coveo/fmt-maven-plugin](https://github.com/coveo/fmt-maven-plugin)
    *   [talios/googleformatter-maven-plugin](https://github.com/talios/googleformatter-maven-plugin)
    *   [Cosium/maven-git-code-format](https://github.com/Cosium/maven-git-code-format):
        A maven plugin that automatically deploys google-java-format as a
        pre-commit git hook.
*   [maltzj/google-style-precommit-hook](https://github.com/maltzj/google-style-precommit-hook):
    A pre-commit (pre-commit.com) hook that will automatically run GJF whenever
    you commit code to your repository

### as a library

The formatter can be used in software which generates java to output more
legible java code. Just include the library in your maven/gradle/etc.
configuration.

#### Maven

```xml
<dependency>
  <groupId>com.palantir.javaformat</groupId>
  <artifactId>palantir-java-format</artifactId>
</dependency>
```

#### Gradle

```groovy
dependencies {
  compile 'com.palantir.javaformat:palantir-java-format:<version>'
}
```

You can then use the formatter through the `formatSource` methods. E.g.

```java
String formattedSource = new Formatter(JavaFormatterOptions.builder().style(Style.PALANTIR).build())
        .formatSource(sourceString);
```

or

```java
CharSource source = ...
CharSink output = ...
new Formatter(JavaFormatterOptions.builder().style(Style.PALANTIR).build())
        .formatSource(source, output);
```

Your starting point should be the instance methods of
`com.palantir.javaformat.java.Formatter`.

## Building from source

```
./gradlew publishToMavenLocal
```

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