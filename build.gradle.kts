import com.google.protobuf.gradle.ExecutableLocator

val arendPackage = "org.arend"
group = arendPackage
version = "1.2.0"

plugins {
    java
    idea
    antlr
    id("com.google.protobuf") version "0.8.8"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.11.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation("commons-cli:commons-cli:1.4")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.1")

    testImplementation("junit:junit:4.12")
    testImplementation("org.hamcrest:hamcrest-library:1.3")

    antlr("org.antlr:antlr4:4.7.2")
    implementation("org.antlr:antlr4-runtime:4.7.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val genSrcDir = projectDir.resolve("src/gen")
val genSrcJavaDir = genSrcDir.resolve("main/java")

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
        genSrcJavaDir.resolve("org/arend/prelude")
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
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it as Any else zipTree(it) })
    from(sourceSets["main"].output)
    dependsOn("prelude")
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

tasks.withType<AntlrTask> {
    outputDirectory = genSrcJavaDir
    arguments.addAll(listOf(
            "-package", "$arendPackage.frontend.parser",
            "-no-listener",
            "-visitor"
    ))
}

protobuf.protobuf.run {
    generatedFilesBaseDir = genSrcDir.toString()
    protoc(closureOf<ExecutableLocator> {
        artifact = "com.google.protobuf:protoc:3.7.1"
    })
}

tasks.withType<Wrapper> {
    gradleVersion = "5.5.1"
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

fun isTrue(name: String) = (extra.properties[name] as? String)?.toBoolean() == true
