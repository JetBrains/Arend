# Arend proof assistant

[![JetBrains incubator project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Actions Status](https://github.com/JetBrains/Arend/workflows/gradle/badge.svg)](https://github.com/JetBrains/Arend/actions)
[![Gitter](https://badges.gitter.im/arend-lang/community.svg)](https://gitter.im/arend-lang/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![](https://jitpack.io/v/JetBrains/Arend.svg)](https://jitpack.io/#JetBrains/Arend)

Arend is a theorem prover based on [Homotopy Type Theory](https://ncatlab.org/nlab/show/homotopy+type+theory).
Visit [arend-lang.github.io](https://arend-lang.github.io/) for more information about the Arend language.

## Usage

Arend is under active development, so you may expect to depend your project on
a development version of Arend, say, the SNAPSHOT version.
This is possible via [JitPack](https://jitpack.io/#JetBrains/Arend/-SNAPSHOT),
simply add this to your `build.gradle`:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        // Open API for writing Arend extensions
        implementation 'com.github.JetBrains.Arend:api:master-SNAPSHOT'
        // The generated ANTLR parser
        implementation 'com.github.JetBrains.Arend:parser:master-SNAPSHOT'
        // The generated protobuf classes
        implementation 'com.github.JetBrains.Arend:proto:master-SNAPSHOT'
        // The main compiler
        implementation 'com.github.JetBrains.Arend:base:master-SNAPSHOT'
    }
}
```

## Building

We use gradle to build the compiler. It comes with a wrapper script (`gradlew` or `gradlew.bat` in
the root of the repository) which downloads appropriate version of gradle
automatically as long as you have JDK installed.

Common tasks are

- `./gradlew jarDep` — build a jar file which includes all the dependencies which can be found at `cli/build/libs`.
  To see the command line options of the application, run `java -jar cli-[version]-full.jar --help`.
  It's essentially equivalent to `./gradlew :cli:jarDep`.

- `./gradlew :api:assemble` - build Arend extension API jar which can be found at `api/build/libs`.

- `./gradlew test` — run all tests.
