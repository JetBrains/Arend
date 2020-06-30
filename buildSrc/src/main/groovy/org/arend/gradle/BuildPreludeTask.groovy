package org.arend.gradle

import org.gradle.api.tasks.*

class BuildPreludeTask extends JavaExec {
    {
        description = "Builds the prelude cache"
        group = "build"
        main = "${project.group}.frontend.PreludeBinaryGenerator"

        dependsOn(project.tasks.getByName("classes"))
    }

    void deleteArcFile() {
        if (preludeDotArc.exists()) {
            if (!preludeDotArc.delete()) {
                println "Failed to delete $preludeDotArc"
            }
        }
    }

    @Input
    final String projectVersion = project.version

    @InputFile
    final File preludeDotArd = project.rootProject.file("lib/Prelude.ard")

    @OutputFile
    final File preludeDotArc = project.rootProject.file("lib/Prelude.arc")

    @InputDirectory
    final File protoDefinitions = project.rootProject.file("proto/src/main/proto")

    @InputFile
    final File preludeDotJava = project.rootProject.file("base/src/main/java/org/arend/prelude/Prelude.java")
}
