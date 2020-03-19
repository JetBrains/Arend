plugins {
    java
    idea
    `java-library`
    `maven-publish`
    signing
}

var annotationsVersion: String by rootProject.ext
var protobufVersion: String by rootProject.ext
var antlrVersion: String by rootProject.ext

annotationsVersion = "19.0.0"
protobufVersion = "3.11.4"
antlrVersion = "4.8"

allprojects {
    group = "org.arend"
    version = "1.2.0"
    repositories {
        jcenter()
        mavenCentral()
    }

    apply {
        plugin("java")
        plugin("idea")
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

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

subprojects {
    apply {
        plugin("signing")
        plugin("maven-publish")
        plugin("java-library")
    }

    java {
        withSourcesJar()
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

    signing {
        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.2.1"
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
