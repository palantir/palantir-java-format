# Contributing

The team welcomes contributions!  To make changes:

- Fork the repo and make a branch
- Write your code (ideally with tests) and make sure the CircleCI build passes
- Open a PR (optionally linking to a github issue)

## Local development

We recommend using [Intellij IDEA Community Edition](https://www.jetbrains.com/idea/) for Java projects. You'll need Java 8 on your machine.

1. Fork the repository
1. Generate the IDE configuration: `./gradlew idea`
1. Import projects into Intellij: `open *.ipr`

Tips:

- run `./gradlew checkstyleMain checkstyleTest` locally to make sure your code conforms to the code-style.

## Working on `:idea-plugin`

Tip: run `./gradlew runIde` to spin up an instance of IntelliJ with the plugin applied.
