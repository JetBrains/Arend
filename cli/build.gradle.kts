dependencies {
    val antlrVersion: String by rootProject.ext
    implementation("commons-cli:commons-cli:1.4")

    val jacksonVersion = "2.11.2"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    val jlineVersion = "3.21.0"
    implementation("org.jline:jline-terminal:$jlineVersion")
    implementation("org.jline:jline-terminal-jansi:$jlineVersion")
    implementation("org.jline:jline-reader:$jlineVersion")
    // implementation("org.jline:jline-builtins:$jlineVersion")

    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation(project(":base"))
    implementation(project(":parser"))
}

// Prelude stuff

val buildPrelude = tasks.register<org.arend.gradle.BuildPreludeTask>("buildPrelude") {
    classpath = sourceSets["main"].runtimeClasspath
    workingDir(rootProject.rootDir)
}

val copyPrelude = tasks.register<Copy>("copyPrelude") {
    dependsOn(buildPrelude)
    from(rootProject.file("lib"))
    into(buildDir.resolve("classes/java/main/lib"))
}

val jarDep = tasks.register<Jar>("jarDep") {
    group = "build"
    manifest.attributes["Main-Class"] = "${project.group}.frontend.ConsoleMain"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
    from(sourceSets.main.get().output)
    archiveClassifier.set("full")
}

val copyJarDep = tasks.register<Copy>("copyJarDep") {
    val jarDep = jarDep.get()
    dependsOn(jarDep)
    from(jarDep.archiveFile.get().asFile)
    into(System.getProperty("user.dir"))
    outputs.upToDateWhen { false }
}

tasks.withType<Jar>().configureEach { dependsOn(copyPrelude) }
