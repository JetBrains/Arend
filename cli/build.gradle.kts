dependencies {
    val antlrVersion: String by rootProject.ext
    implementation("commons-cli:commons-cli:1.4")

    val jacksonVersion = "2.10.3"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    val jlineVersion = "3.15.0"
    implementation("org.jline:jline-terminal:$jlineVersion")
    implementation("org.jline:jline-terminal-jansi:$jlineVersion")
    implementation("org.jline:jline-reader:$jlineVersion")
    // implementation("org.jline:jline-builtins:$jlineVersion")

    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation(project(":base"))
    implementation(project(":parser"))
}

// Prelude stuff

val buildPrelude = task<org.arend.gradle.BuildPreludeTask>("buildPrelude") {
    classpath = sourceSets["main"].runtimeClasspath
    workingDir(rootProject.rootDir)
}

val copyPrelude = task<Copy>("copyPrelude") {
    dependsOn(buildPrelude)
    from(rootProject.file("lib"))
    into(buildDir.resolve("classes/java/main/lib"))
}

val jarDep = task<Jar>("jarDep") {
    group = "build"
    manifest.attributes["Main-Class"] = "${project.group}.frontend.ConsoleMain"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it as Any else zipTree(it) })
    from(sourceSets["main"].output)
    archiveClassifier.set("full")
}

val copyJarDep = task<Copy>("copyJarDep") {
    dependsOn(jarDep)
    from(jarDep.archiveFile.get().asFile)
    into(System.getProperty("user.dir"))
    outputs.upToDateWhen { false }
}

tasks.withType<Jar> { dependsOn(copyPrelude) }
