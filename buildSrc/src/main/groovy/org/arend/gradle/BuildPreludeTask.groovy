package org.arend.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec

class BuildPreludeTask extends JavaExec {
    {
        description = "Builds the prelude cache"
        group = "build"
        main = "${project.group}.frontend.PreludeBinaryGenerator"
    }

    @Input
    String projectVersion = project.version
}
