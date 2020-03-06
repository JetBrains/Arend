import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateVersionTask extends DefaultTask {
    @Input
    Object taskVersion = project.version
    @Input
    String basePackage = "org.arend"
    @OutputDirectory
    File outputDir = new File("src/main/java/org/arend/prelude")

    @TaskAction
    def run() {
        def className = "GeneratedVersion"
        def code = """\
            package ${basePackage}.prelude;
            import ${basePackage}.util.Version;
            public class $className {
              public static final Version VERSION = new Version("$taskVersion");
            }""".stripIndent()
        outputDir.mkdirs()
        def outFile = new File(outputDir, "${className}.java")
        if (!outFile.exists())
            assert outFile.createNewFile()
        outFile.write(code)
    }
}
