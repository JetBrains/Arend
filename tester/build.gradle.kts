plugins {
    java
}

dependencies {
    val annotationsVersion: String by rootProject.ext
    val antlrVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")

    implementation(project(":api"))
    implementation(project(":base"))
    implementation(project(":parser"))
    implementation(project(":cli"))

    implementation("junit:junit:4.12")
    implementation("org.hamcrest:hamcrest-library:1.3")
}
