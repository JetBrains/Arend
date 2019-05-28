import com.google.protobuf.gradle.ExecutableLocator

plugins {
    java
    idea
    antlr
    id("com.google.protobuf") version "0.8.8"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compile("commons-cli:commons-cli:1.4")
    compile("com.google.guava:guava:27.1-jre")
    compile("com.google.protobuf:protobuf-java:3.7.1")
    compile("com.google.code.findbugs:jsr305:3.0.2")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.9")

    testCompile("junit:junit:4.12")
    testCompile("org.hamcrest:hamcrest-library:1.3")

    antlr("org.antlr:antlr4:4.7.2")
    implementation("org.antlr:antlr4-runtime:4.7.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

val arendPackage = "org.arend"

task<Jar>("jarDep") {
    manifest.attributes["Main-Class"] = "$arendPackage.frontend.ConsoleMain"
    from(configurations.runtimeClasspath.map { if (it.isDirectory) it as Any else zipTree(it) })
    from(java.sourceSets["main"].output)
}

tasks.withType<Jar> {
    dependsOn("prelude")
}

val genSrcDir = file("src/gen")

java.sourceSets {
    getByName("main").java.srcDirs(genSrcDir)
}

idea {
    module {
        generatedSourceDirs.add(genSrcDir)
        outputDir = file("$buildDir/classes/java/main")
        testOutputDir = file("$buildDir/classes/java/test")
    }
}

tasks.withType<AntlrTask> {
    outputDirectory = genSrcDir
    arguments.addAll(listOf(
            "-package", "$arendPackage.frontend.parser",
            "-no-listener",
            "-visitor"
    ))
}

protobuf.protobuf.run {
    generatedFilesBaseDir = genSrcDir.toString()
    protoc(closureOf<ExecutableLocator> {
        artifact = "com.google.protobuf:protoc:3.7.1"
    })
}

tasks.withType<Wrapper> {
    gradleVersion = "4.9"
}


// Prelude stuff

val preludeOutputDir = "$buildDir/classes/java/main"

task<Copy>("copyPrelude") {
    from("lib/Prelude.ard")
    into("$preludeOutputDir/lib")
}

task<JavaExec>("prelude") {
    description = "Builds the prelude cache"
    group = "Build"
    main = "$arendPackage.prelude.PreludeBinaryGenerator"
    classpath = java.sourceSets["main"].runtimeClasspath
    args = listOf(preludeOutputDir)
    dependsOn("copyPrelude")
}
