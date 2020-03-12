plugins {
    java
    idea
}

dependencies {
    val antlrVersion: String by rootProject.ext
    implementation("org.antlr:antlr4-runtime:$antlrVersion")
}

idea {
    module {
        generatedSourceDirs.add(file("src/main/java"))
        outputDir = file("$buildDir/classes/java/main")
        testOutputDir = file("$buildDir/classes/java/test")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
