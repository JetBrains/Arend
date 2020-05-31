plugins {
    idea
}

dependencies {
    val annotationsVersion: String by rootProject.ext
    val protobufVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation(project(":api"))
    implementation(project(":proto"))
}

val genDir = file("src/main/gen")
val generateVersion = task<org.arend.gradle.GenerateVersionTask>("generateVersion") {
    basePackage = project.group.toString()
    outputDir = genDir.resolve("org/arend/prelude")
}

idea {
    module {
        generatedSourceDirs.add(genDir)
    }
}

sourceSets {
    main {
        java {
            srcDirs(genDir)
        }
    }
}

tasks.withType<JavaCompile> {
    dependsOn(generateVersion)
}
