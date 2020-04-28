plugins {
    java
}

dependencies {
    val annotationsVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
    implementation(project(":api"))
    implementation(project(":base"))
}
