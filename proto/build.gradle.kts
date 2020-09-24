plugins {
    id("com.google.protobuf") version "0.8.13"
}

val protobufVersion: String by rootProject.ext
dependencies {
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
}

val sourceDir = file("src/main/java")

idea {
    module {
        generatedSourceDirs.add(sourceDir)
    }
}

protobuf.protobuf.apply {
    generatedFilesBaseDir = sourceDir.toString()
    protoc(closureOf<com.google.protobuf.gradle.ExecutableLocator> {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    })
}
