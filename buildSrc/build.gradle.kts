plugins {
    java
    groovy
    antlr
}

repositories { jcenter() }

tasks.withType<AntlrTask> {
    outputDirectory = projectDir.parentFile.resolve("parser/src/main/java")
    arguments.addAll(listOf(
        "-package", "org.arend.frontend.parser",
        "-no-listener",
        "-visitor"
    ))
}

dependencies {
    antlr("org.antlr:antlr4:4.8")
}