import com.google.protobuf.gradle.ExecutableLocator

val arendPackage = "org.arend"
group = arendPackage
version = "1.2.0"

plugins {
    java
    idea
    id("com.google.protobuf") version "0.8.11"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.11.4")

    implementation("commons-cli:commons-cli:1.4")

    val jacksonVersion = "2.10.3"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("org.jetbrains:annotations:19.0.0")

    testImplementation("junit:junit:4.12")
    testImplementation("org.hamcrest:hamcrest-library:1.3")

    implementation("org.antlr:antlr4-runtime:4.8")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val genSrcDir = projectDir.resolve("src/gen")

val generateVersion = task("generateVersion") {
    doFirst {
        val className = "GeneratedVersion"
        val code = """
            package $arendPackage.prelude;
            import org.arend.util.Version;
            public class $className {
              public static final Version VERSION = new Version("$version");
            }
        """.trimIndent()
        genSrcDir.resolve("main/java/org/arend/prelude")
            .apply { mkdirs() }
            .resolve("$className.java")
            .apply { if (!exists()) createNewFile() }
            .writeText(code)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    dependsOn(generateVersion)
}

task<Jar>("jarDep") {
    manifest.attributes["Main-Class"] = "$arendPackage.frontend.ConsoleMain"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it as Any else zipTree(it) })
    from(sourceSets["main"].output)
    archiveClassifier.set("full")
    dependsOn("prelude")
}

val apiClasses = task<Jar>("apiClasses") {
    archiveBaseName.set("${project.name}-api")
    from(sourceSets["main"].output) {
        include("org/arend/ext/**")
    }
}

val apiSources = task<Jar>("apiSources") {
    archiveBaseName.set("${project.name}-api")
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource) {
        include("org/arend/ext/**")
    }
}

task("api") {
    dependsOn(apiSources, apiClasses)
}

task<Jar>("jarSrc") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource) {
        exclude("**/frontend/**")
    }
}

tasks.getByName<Jar>("jar") {
    exclude("**/frontend/**")
}

sourceSets {
    main {
        java {
            srcDirs(genSrcDir)
            if (isTrue("AREND_EXCLUDE_CONSOLE")) {
                exclude("**/frontend/**")
            }
        }
    }
}

idea {
    module {
        generatedSourceDirs.add(genSrcDir)
        outputDir = file("$buildDir/classes/java/main")
        testOutputDir = file("$buildDir/classes/java/test")
    }
}

protobuf.protobuf.run {
    generatedFilesBaseDir = genSrcDir.toString()
    protoc(closureOf<ExecutableLocator> {
        artifact = "com.google.protobuf:protoc:3.11.4"
    })
}

tasks.withType<Wrapper> {
    gradleVersion = "6.2.1"
}


// Prelude stuff

val preludeOutputDir = "$buildDir/classes/java/main"

val copyPrelude = task<Copy>("copyPrelude") {
    from("lib/Prelude.ard")
    into("$preludeOutputDir/lib")
}

task<JavaExec>("prelude") {
    description = "Builds the prelude cache"
    group = "Build"
    main = "$arendPackage.frontend.PreludeBinaryGenerator"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(preludeOutputDir)
    dependsOn(copyPrelude)
}


// Utils

fun isTrue(name: String) = extra.properties[name] == "true"
