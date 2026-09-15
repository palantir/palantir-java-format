# CLAUDE.md

Guidance for Claude Code (claude.ai/code) and other AI coding agents working in this repository.

## What this project is

`palantir-java-format` is a deterministic, 120-column Java source formatter, forked from
[google-java-format](https://github.com/google/google-java-format). It is published as a library, a CLI, a Gradle
plugin, an IntelliJ plugin, an Eclipse plugin, and a GraalVM native image.

Because it rewrites source files in place in people's editors and in CI, **a formatting change must never change the
meaning of the program.** Treat any behaviour that can alter semantics (import removal, string wrapping, comment
re-flow) as correctness-critical, not cosmetic.

## Build and test

The build provisions its own JDKs via `palantir/gradle-jdks` — you do **not** need a system JDK, and
`./gradlew` bootstraps everything on first run (it downloads Amazon Corretto 17/21 and GraalVM 23 into
`~/.gradle/gradle-jdks`). The first invocation takes several minutes.

```bash
./gradlew build                             # compile + test + all checks (what CI runs)
./gradlew :palantir-java-format:test        # core formatter tests only
./gradlew :palantir-java-format:test --tests '*RemoveUnusedImports*'   # a single test class
./gradlew format                            # apply this formatter to the repo's own sources
./gradlew test -Drecreate=true              # regenerate checked-in expected outputs (see below)
```

Notes:

- Checkstyle is disabled repo-wide (`build.gradle`); style is enforced by the formatter itself plus error-prone.
- `:idea-plugin:build` needs to download an IntelliJ distribution and is slow; `./gradlew build -x :idea-plugin:build`
  is a reasonable local shortcut, but confirm the full build before calling a change done.
- CI additionally runs `./gradlew nativeCompile` and asserts **no git-tracked file was modified by the build**. If a
  build step rewrites a checked-in file, commit that file.
- Dependency versions are managed by `gradle-consistent-versions`: edit `versions.props`, then run
  `./gradlew --write-locks` to update `versions.lock`. Never hand-edit `versions.lock`.
- Targets Java 17 (`libraryTarget`), runs on 21 (`runtime`).

## Module map

| Module | Purpose |
| --- | --- |
| `palantir-java-format` | The formatter itself. Almost all changes belong here. |
| `palantir-java-format-spi` | Stable service-provider interface used by the editor plugins. |
| `palantir-java-format-jdk-bootstrap` | Reflective bootstrap for the `jdk.compiler` internals the formatter needs. |
| `gradle-palantir-java-format` | The `com.palantir.java-format` Gradle plugin (also wires up Spotless and IDEA). |
| `idea-plugin`, `eclipse_plugin` | Editor integrations. |
| `palantir-java-format-native` | GraalVM native-image CLI. |
| `palantir-java-format-benchmarks` | JMH benchmarks. |
| `debugger` | Web UI for inspecting the formatter's `Doc` tree. |

## Architecture of the formatter

The pipeline in `com.palantir.javaformat` is:

1. **Parse** — `Formatter` drives javac to produce a `JCCompilationUnit`; `JavaInput` tokenizes the original source and
   keeps the mapping from tokens to character offsets, so comments and original positions survive.
2. **Emit ops** — `JavaInputAstVisitor` (plus the language-level subclasses in `java/java14` and `java/java21`) walks
   the AST and emits `Op`s through `OpsBuilder`. This is where "how should this construct be laid out" is decided.
3. **Build the doc tree** — `doc/DocBuilder` turns the op stream into a tree of `Doc` nodes (`Level`, `Break`,
   `Token`, `Comment`).
4. **Lay out** — each `Level` decides whether it fits on one line, driven by `BreakBehaviour`,
   `LastLevelBreakability` and `PartialInlineability`. This is the heart of palantir-java-format's divergence from
   google-java-format.
5. **Write** — `JavaOutput` renders the laid-out doc back to text and produces `Replacement`s.

Separate, self-contained passes that run alongside formatting: `RemoveUnusedImports`, `ImportOrderer`,
`ModifierOrderer`, `StringWrapper`, `JavaCommentsHelper`.

If a change affects layout decisions, the `debugger/` module and `DebugRenderer` let you see the `Doc` tree and why a
level broke.

### Single-file analysis is a hard constraint

The formatter sees **one compilation unit at a time**, with no classpath, no sourcepath, and no symbol resolution — it
parses but does not attribute. Any pass that reasons about what a simple name refers to must therefore be
conservative: when you cannot prove an edit is semantics-preserving, don't make the edit. `RemoveUnusedImports`
documents a concrete instance of this (a `java.lang` single-type import can shadow a same-package type that this file
cannot see).

## Tests

- **File-based golden tests** are the main tool. Add a matching `Foo.input` / `Foo.output` pair under
  `palantir-java-format/src/test/resources/com/palantir/javaformat/java/testdata/` and `FormatterIntegrationTest`
  picks it up automatically. `testimports/` and `testjavadoc/` work the same way for their suites.
  Regenerate expected outputs with `./gradlew test -Drecreate=true`, then **read the resulting diff** — `recreate`
  will happily bless a regression.
- **Parameterized unit tests** (e.g. `RemoveUnusedImportsTest`) hold an array of `{input, expectedOutput}` line
  arrays. Add a case to the array rather than writing a new test method.
- The formatter must be **idempotent**: formatting an already-formatted file is a no-op. Tests enforce this, so a
  change that oscillates will fail.
- When a bug report is about code that stops compiling, prove it with `javac` before and after the fix instead of
  reasoning from the JLS alone.

## Conventions

- Code is formatted by this project's own formatter — run `./gradlew format` before committing.
- Files keep their existing license header; files inherited from google-java-format keep the Google copyright.
- `@SuppressWarnings("for-rollout:...")` markers are error-prone rollout suppressions managed by tooling; leave them
  alone unless you are fixing the underlying issue.
- Comments in this codebase explain *why* a layout or shadowing rule exists, often citing the JLS. Match that: a
  non-obvious correctness rule should carry a short justification and, where relevant, the issue number.
- Follow `CONTRIBUTING.md`: fork, branch, tests, PR. Releases are automated (autorelease), so do not bump versions by
  hand and do not edit generated files such as `.circleci/config.yml`.
