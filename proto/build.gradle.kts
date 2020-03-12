plugins {
    java
    idea
    id("com.google.protobuf") version "0.8.11"
}

val protobufVersion: String by rootProject.ext
dependencies {
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

val sourceDir = file("src/main/java")

idea {
    module {
        generatedSourceDirs.add(sourceDir)
        outputDir = file("$buildDir/classes/java/main")
        testOutputDir = file("$buildDir/classes/java/test")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

protobuf.protobuf.apply {
    generatedFilesBaseDir = sourceDir.toString()
    protoc(closureOf<com.google.protobuf.gradle.ExecutableLocator> {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    })
}
