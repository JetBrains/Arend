dependencies {
    val annotationsVersion: String by rootProject.ext
    implementation("org.jetbrains:annotations:$annotationsVersion")
    implementation(project(":cli"))
    implementation(project(":base"))
    implementation(project(":parser"))
}
