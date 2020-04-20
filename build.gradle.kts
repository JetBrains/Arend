plugins {
    java
    idea
    `java-library`
    `maven-publish`
}

var annotationsVersion: String by rootProject.ext
var protobufVersion: String by rootProject.ext
var antlrVersion: String by rootProject.ext

annotationsVersion = "19.0.0"
protobufVersion = "3.11.4"
antlrVersion = "4.8"

allprojects {
    group = "org.arend"
    version = "1.3.0"
    repositories {
        jcenter()
        mavenCentral()
    }

    apply {
        plugin("java")
        plugin("idea")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    idea {
        module {
            outputDir = file("$buildDir/classes/java/main")
            testOutputDir = file("$buildDir/classes/java/test")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isDeprecation = true
        options.compilerArgs.add("-Xlint:unchecked")
    }
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("java-library")
    }

    java {
        withSourcesJar()
        // Enable on-demand
        // withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = this@subprojects.group.toString()
                version = this@subprojects.version.toString()
                artifactId = this@subprojects.name
                from(components["java"])
                pom {
                    url.set("https://arend-lang.github.io")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://github.com/JetBrains/Arend/blob/master/LICENSE")
                        }
                    }
                }
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.3"
}

dependencies {
    testImplementation("org.jetbrains:annotations:$annotationsVersion")
    testImplementation("org.antlr:antlr4-runtime:$antlrVersion")

    testImplementation(project(":api"))
    testImplementation(project(":base"))
    testImplementation(project(":parser"))
    testImplementation(project(":cli"))

    testImplementation("junit:junit:4.12")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
}
