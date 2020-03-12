plugins {
    java
    idea
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

idea {
    module {
        outputDir = file("$buildDir/classes/java/main")
        testOutputDir = file("$buildDir/classes/java/test")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Prelude stuff

val buildPrelude = task<JavaExec>("buildPrelude") {
    description = "Builds the prelude cache"
    group = "Build"
    main = "${project.group}.frontend.PreludeBinaryGenerator"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir(rootProject.rootDir)
    args = listOf(".")
}

val copyPrelude = task<Copy>("copyPrelude") {
    dependsOn(buildPrelude)
    from("lib")
    from(project(":base").buildDir.resolve("classes/main/resources"))
}

task<Jar>("jarDep") {
    manifest.attributes["Main-Class"] = "${project.group}.frontend.ConsoleMain"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it as Any else zipTree(it) })
    from(sourceSets["main"].output)
    archiveClassifier.set("full")
    dependsOn(copyPrelude)
}
