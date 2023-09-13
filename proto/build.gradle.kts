plugins {
    id("com.google.protobuf") version "0.9.4"
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
}
