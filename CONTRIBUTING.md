* [Building](#building)
  * [Common Gradle Tasks](#common-gradle-tasks)
  * [Developing in IntelliJ IDEA](#developing-in-intellij-idea)
* [Project Structure](#project-structure)

<!--
Created by [gh-md-toc](https://github.com/ekalinin/github-markdown-toc)
-->

# Building

We use gradle to build the compiler. It comes with a wrapper script (`gradlew` or `gradlew.bat` in
the root of the repository) which downloads appropriate version of gradle
automatically as long as you have JDK installed.

## Common Gradle Tasks

|Command|Description|
|:---|:---|
|`./gradlew :cli:jarDep`|build a jar file which includes all the dependencies which can be found at `cli/build/libs`.<br/>A short-hand version of this task is `./gradlew jarDep`.|
|`./gradlew :api:assemble`|build Arend extension API jar which can be found at `api/build/libs`.|
|`./gradlew test`|run all tests.|

To see the command line options of the application, run `java -jar cli-[version]-full.jar --help`
after running the `jarDep` task.

## Developing in IntelliJ IDEA

Here's an [instruction](https://www.jetbrains.com/help/idea/gradle.html)
on how to work with gradle projects in IntelliJ IDEA.

You may also need the following plugins:
+ Gradle and Groovy -- bundled plugins, needed for building the project
+ [ANTLR v4 grammar](https://plugins.jetbrains.com/plugin/7358) for editing the parser
+ [Protobuf](https://plugins.jetbrains.com/plugin/8277) for editing the serialized protobuf
+ [Kotlin](https://plugins.jetbrains.com/plugin/6954) for editing the build scripts
+ [Arend](https://plugins.jetbrains.com/plugin/11162) for editing Arend code

# Project Structure

Arend is split into several subprojects:

|Subproject|Description|
|:---:|:---|
|`buildSrc`|built before the project is built.<br/>This subproject runs the ANTLR parser generator.|
|`parser`|the generated ANTLR parser (the generation is done in `buildSrc`)|
|`proto`|generated protobuf classes.|
|`api`|open API for writing Arend extensions.|
|`base`|the Arend typechecker.<br/>It depends on `api`, `proto`.|
|`cli`|the CLI frontend of Arend with the ANTLR parser.<br/>It depends on `base`, `parser`, `api`, `proto`.|

The purpose of `parser` is to avoid introducing the dependency of the ANTLR
generator to other subprojects which only requires
the generated parser along with a small ANTLR runtime
(since it's a dependency of `buildSrc` instead of `parser`).

The root project contains all the tests,
and it depends on project `cli`.
