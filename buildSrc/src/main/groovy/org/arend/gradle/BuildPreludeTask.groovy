package org.arend.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile

class BuildPreludeTask extends JavaExec {
    {
        description = "Builds the prelude cache"
        group = "build"
        main = "${project.group}.frontend.PreludeBinaryGenerator"
    }

    @Input
    final String projectVersion = project.version

    @InputFile
    final File preludeDotArd = project.rootProject.file("lib/Prelude.ard")

    @OutputFile
    final File preludeDotArc = project.rootProject.file("lib/Prelude.arc")
}
