dependencies {
    val protobufVersion: String by rootProject.ext
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation(project(":proto"))

    api(project(":api"))
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
