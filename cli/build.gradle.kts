plugins {
    java
}

dependencies {
    val annotationsVersion: String by rootProject.ext
    val antlrVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
    implementation("commons-cli:commons-cli:1.4")

    val jacksonVersion = "2.10.3"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation(project(":api"))
    implementation(project(":base"))
    implementation(project(":parser"))
}

// Prelude stuff

val buildPrelude = task<org.arend.gradle.BuildPreludeTask>("buildPrelude") {
    classpath = sourceSets["main"].runtimeClasspath
    workingDir(rootProject.rootDir)
    args = listOf(".")
}

val copyPrelude = task<Copy>("copyPrelude") {
    dependsOn(buildPrelude)
    from(rootProject.file("lib"))
    into(buildDir.resolve("classes/java/main/lib"))
}

task<Jar>("jarDep") {
    manifest.attributes["Main-Class"] = "${project.group}.frontend.ConsoleMain"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it as Any else zipTree(it) })
    from(sourceSets["main"].output)
    archiveClassifier.set("full")
    dependsOn(copyPrelude)
}
