# palantir-java-format Eclipse Plugin

## Installation

1. Run `./gradlew :eclipse_plugin:build` in the main folder,
1. If running Eclipse under JRE 17 or later add these options to `eclipse.ini` after `-vmargs`:
   ```
   --add-exports
   jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
   --add-exports
   jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
   --add-exports
   jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
   --add-exports
   jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
   --add-exports
   jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
   ```
1. Copy `build/libs/palantir-java-format-eclipse-plugin-<version>.jar` to the `dropins` folder of your Eclipse installation,
1. Run `eclipse -clean`.
