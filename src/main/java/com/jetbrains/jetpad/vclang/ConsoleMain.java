package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.antlr.v4.runtime.*;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleMain {
  public static void main(String[] args) {
    Options cmdOptions = new Options();
    cmdOptions.addOption("h", "help", false, "print this message");
    cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("dir").desc("source directory").build());
    cmdOptions.addOption(Option.builder("o").longOpt("output").hasArg().argName("dir").desc("output directory").build());
    cmdOptions.addOption(Option.builder("L").hasArg().argName("dir").desc("add <dir> to the list of directories searched for libraries").build());
    cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
    CommandLineParser cmdParser = new DefaultParser();
    CommandLine cmdLine;
    try {
      cmdLine = cmdParser.parse(cmdOptions, args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      return;
    }

    if (cmdLine.hasOption("h")) {
      new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
    }

    final Path sourceDir = Paths.get(cmdLine.getOptionValue("s"));
    final Path outputDir = Paths.get(cmdLine.getOptionValue("o"));
    boolean recompile = cmdLine.hasOption("recompile");

    List<File> libDirs = new ArrayList<>();
    String workingDir = System.getenv("AppData");
    File workingPath = null;
    if (workingDir != null) {
      workingPath = new File(workingDir, "vclang");
    } else {
      workingDir = System.getProperty("user.home");
      if (workingDir != null) {
        workingPath = new File(workingDir, ".vclang");
      }
    }
    if (cmdLine.getOptionValues("L") != null) {
      for (String dir : cmdLine.getOptionValues("L")) {
        libDirs.add(new File(dir));
      }
    }
    if (workingPath != null) {
      libDirs.add(new File(workingPath, "lib"));
    }

    if (cmdLine.getArgList().isEmpty()) {
      try {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            if (path.getFileName().toString().endsWith(".vc")) {
              processFile(path, sourceDir, outputDir);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            System.err.println("I/O error: " + e.getMessage());
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        System.err.println("I/O error: " + e.getMessage());
      }
    } else {
      for (String fileName : cmdLine.getArgList()) {
        processFile(Paths.get(fileName), sourceDir, outputDir);
      }
    }
  }

  static private List<String> getNames(Path file) {
    int nameCount = file.getNameCount();
    List<String> names = new ArrayList<>(nameCount);
    for (int i = 0; i < nameCount; ++i) {
      String name = file.getName(i).toString();
      if (i == nameCount - 1) {
        if (!name.endsWith(".vc")) return null;
        name = name.substring(0, name.length() - 3);
      }

      if (name.length() == 0 || !(Character.isLetterOrDigit(name.charAt(0)) || name.charAt(0) == '_')) return null;
      for (int j = 1; j < name.length(); ++j) {
        if (!(Character.isLetterOrDigit(name.charAt(j)) || name.charAt(j) == '_' || name.charAt(j) == '-' || name.charAt(j) == '\'')) return null;
      }
      names.add(name);
    }
    return names;
  }

  static private Module getModule(List<String> moduleNames) {
    // TODO
    return null;
  }

  static private void processFile(Path fileName, Path sourceDir, Path outputDir) {
    Path relativePath = null;
    String moduleName = null;
    List<String> moduleNames = null;
    if (fileName.startsWith(sourceDir)) {
      relativePath = sourceDir.relativize(fileName);
      moduleNames = getNames(relativePath);
      if (moduleNames == null || moduleNames.size() == 0) {
        relativePath = null;
      } else {
        moduleName = moduleNames.get(moduleNames.size() - 1);
        moduleNames.set(moduleNames.size() - 1, moduleName + ".vcc");
        for (int i = 0; i < moduleNames.size() - 1; ++i) {
          outputDir = outputDir.resolve(moduleNames.get(i));
        }
      }
    }

    ANTLRInputStream input;
    try {
      input = new ANTLRInputStream(new FileInputStream(fileName.toFile()));
    } catch (IOException e) {
      System.err.println("I/O error: " + e.getMessage());
      return;
    }

    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    BuildVisitor builder = new BuildVisitor();
    final List<ParserError> parserErrors = builder.getErrors();
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        parserErrors.add(new ParserError(new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.DefsContext tree = parser.defs();
    List<Concrete.Definition> defs = parserErrors.isEmpty() ? builder.visitDefs(tree) : null;
    if (!parserErrors.isEmpty()) {
      for (ParserError error : parserErrors) {
        System.err.println(error);
      }
      return;
    }

    Map<String, Definition> context = Prelude.getDefinitions();
    List<TypeCheckingError> errors = new ArrayList<>();
    Concrete.ClassDefinition classDef = new Concrete.ClassDefinition(new Concrete.Position(0, 0), moduleName, new Universe.Type(), defs);
    ClassDefinition typedClassDef = new DefinitionCheckTypeVisitor(getModule(moduleNames), context, errors).visitClass(classDef, new ArrayList<Binding>());

    try {
      ModuleSerialization.writeClass(typedClassDef, outputDir);
    } catch (IOException e) {
      System.err.println("I/O error: " + e.getMessage());
    }

    for (TypeCheckingError error : errors) {
      System.err.print((relativePath != null ? relativePath : fileName) + ": ");
      if (error.getExpression() instanceof Concrete.SourceNode) {
        Concrete.Position position = ((Concrete.SourceNode) error.getExpression()).getPosition();
        System.err.print(position.line + ":" + position.column + ": ");
      }
      System.err.println(error);
    }

    if (errors.isEmpty()) {
      System.out.println("[OK] " + (relativePath != null ? relativePath : fileName));
    }
  }
}
