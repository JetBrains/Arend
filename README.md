# Arend proof assistant

[![JetBrains incubator project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Actions Status](https://github.com/JetBrains/Arend/workflows/gradle/badge.svg)](https://github.com/JetBrains/Arend/actions)
[![Gitter](https://badges.gitter.im/arend-lang/community.svg)](https://gitter.im/arend-lang/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Arend is a theorem prover based on [Homotopy Type Theory](https://ncatlab.org/nlab/show/homotopy+type+theory).
Visit [arend-lang.github.io](https://arend-lang.github.io/) for more information about the Arend language.

## Building

We use gradle to build the plugin. It comes with a wrapper script (`gradlew` or `gradlew.bat` in
the root of the repository) which downloads appropriate version of gradle
automatically as long as you have JDK installed.

Common tasks are

- `./gradlew jarDep` — build a jar file which includes all the dependencies which can be found at `cli/build/libs`.
  To see the command line options of the application, run `java -jar cli-[version]-full.jar --help`.

- `./gradlew :api:assemble` - build Arend extension API jar which can be found at `api/build/libs`.

- `./gradlew test` — run all tests.
