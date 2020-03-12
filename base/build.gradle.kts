plugins {
    java
    idea
    id("com.google.protobuf") version "0.8.11"
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
        outputDir = file("$buildDir/classes/java/main")
        testOutputDir = file("$buildDir/classes/java/test")
    }
}

sourceSets { main { java { srcDirs(genDir) } } }

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    dependsOn(generateVersion)
}
