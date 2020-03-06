import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateVersionTask extends DefaultTask {
    @Input
    Object taskVersion = project.version
    @Input
    String basePackage = "org.arend"
    @Input
    String className = "GeneratedVersion"
    @OutputDirectory
    File outputDir = new File("src/main/java/org/arend/prelude")

    @TaskAction
    def run() {
        def code = """\
            package ${basePackage}.prelude;
            import ${basePackage}.util.Version;
            import org.jetbrains.annotations.NotNull;
            public class $className {
              public static final @NotNull String VERSION_STRING = "$taskVersion";
              public static final @NotNull Version VERSION = new Version(VERSION_STRING);
            }""".stripIndent()
        outputDir.mkdirs()
        def outFile = new File(outputDir, "${className}.java")
        if (!outFile.exists())
            assert outFile.createNewFile()
        outFile.write(code)
    }
}
