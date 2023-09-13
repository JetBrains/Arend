plugins {
    java
    groovy
    antlr
}

repositories {
    mavenCentral()
}

tasks.withType<AntlrTask> {
    outputDirectory = projectDir.parentFile.resolve("parser/src/main/java")
    arguments.addAll(listOf(
        "-package", "org.arend.frontend.parser",
        "-no-listener",
        "-visitor"
    ))
}

dependencies {
    antlr("org.antlr:antlr4:4.10")
}
