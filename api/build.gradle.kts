plugins {
    java
    idea
}

dependencies {
    val annotationsVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
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
