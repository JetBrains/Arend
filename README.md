# Arend proof assistant

[![JetBrains incubator project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Actions Status](https://github.com/JetBrains/Arend/workflows/gradle/badge.svg)](https://github.com/JetBrains/Arend/actions)
[![Gitter](https://badges.gitter.im/arend-lang/community.svg)](https://gitter.im/arend-lang/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![](https://jitpack.io/v/JetBrains/Arend.svg)](https://jitpack.io/#JetBrains/Arend)

Arend is a theorem prover based on [Homotopy Type Theory](https://ncatlab.org/nlab/show/homotopy+type+theory).
Visit [arend-lang.github.io](https://arend-lang.github.io/) for more information about the Arend language.

For instructions on building Arend locally, general description of the codebase,
there's [ARCHITECTURE.md](ARCHITECTURE.md).

## Community

- [Google](https://groups.google.com/forum/#!forum/arend-lang)
- [Telegram](https://t.me/joinchat/GPwwsREtctsqEVs6gPeLLg)
- [Gitter](https://gitter.im/arend-lang/community)

## Usage

Arend is under active development, so you may expect to depend your project on
a development version of Arend,
either a certain git revision or the SNAPSHOT version.
This is possible via [JitPack](https://jitpack.io/#JetBrains/Arend/-SNAPSHOT),
simply add this to your `build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    // The version of Arend -- can be a short revision, "[branch]-SNAPSHOT",
    // "-SNAPSHOT", or a tag (or a release, like "v1.3.0").
    String arendVersion = "master-SNAPSHOT"
    // Open API for writing Arend extensions
    implementation "com.github.JetBrains.Arend:api:$arendVersion"
    // The generated ANTLR parser
    implementation "com.github.JetBrains.Arend:parser:$arendVersion"
    // The generated protobuf classes
    implementation "com.github.JetBrains.Arend:proto:$arendVersion"
    // The main compiler
    implementation "com.github.JetBrains.Arend:base:$arendVersion"
}
```

In case you prefer Gradle Kotlin DSL,
use the following syntax in your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    // The version of Arend
    val arendVersion = "master-SNAPSHOT"
    implementation("com.github.JetBrains.Arend:api:$arendVersion")
    implementation("com.github.JetBrains.Arend:parser:$arendVersion")
    implementation("com.github.JetBrains.Arend:proto:$arendVersion")
    implementation("com.github.JetBrains.Arend:base:$arendVersion")
}
```
