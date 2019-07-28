# Arend proof assistant

[![JetBrains incubator project](http://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Arend is a theorem prover based on [Homotopy Type Theory](https://ncatlab.org/nlab/show/homotopy+type+theory).
Visit [arend-lang.github.io](https://arend-lang.github.io/) for more information about the Arend language.

## Building

We use gradle to build the plugin. It comes with a wrapper script (`gradlew` or `gradlew.bat` in
the root of the repository) which downloads appropriate version of gradle
automatically as long as you have JDK installed.

Common tasks are

  - `./gradlew jarDep` — build a jar file which includes all the dependecies which can be found at `build/libs`.
    To see the command line options of the application, run `java -jar arend.jar --help`.

  - `./gradlew test` — run all tests.
