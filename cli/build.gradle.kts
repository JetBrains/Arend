dependencies {
    val annotationsVersion: String by rootProject.ext
    val antlrVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
    implementation("commons-cli:commons-cli:1.4")

    val jacksonVersion = "2.10.3"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    val jlineVersion = "3.14.1"
    implementation("org.jline:jline-terminal:$jlineVersion")
    implementation("org.jline:jline-terminal-jansi:$jlineVersion")
    implementation("org.jline:jline-reader:$jlineVersion")
    // implementation("org.jline:jline-builtins:$jlineVersion")

    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation(project(":api"))
    implementation(project(":base"))
    implementation(project(":parser"))
}

val execRepl = task<JavaExec>("execRepl") {
    workingDir(rootProject.rootDir)
    classpath = sourceSets["main"].runtimeClasspath
    defaultCharacterEncoding = "UTF-8"
    standardInput = System.`in`
    standardOutput = System.out
    main = "org.arend.frontend.repl.CliReplState"
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
