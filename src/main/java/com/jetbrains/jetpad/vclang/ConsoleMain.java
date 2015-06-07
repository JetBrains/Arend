package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
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

    String sourceDir = cmdLine.getOptionValue("s");
    String outputDir = cmdLine.getOptionValue("o");
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
        Files.walkFileTree(Paths.get(sourceDir), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            processFile(path);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            System.err.println(path + ": I/O error: " + e.getMessage());
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        System.err.println(sourceDir + ": I/O error: " + e.getMessage());
      }
    } else {
      for (String fileName : cmdLine.getArgList()) {
        processFile(Paths.get(fileName));
      }
    }
  }

  static void processFile(Path fileName) {
    ANTLRInputStream input;
    try {
      input = new ANTLRInputStream(new FileInputStream(fileName.toFile()));
    } catch (IOException e) {
      System.err.println(fileName + ": I/O error: " + e.getMessage());
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
    for (Abstract.Definition def : defs) {
      Definition typedDef = def.accept(new DefinitionCheckTypeVisitor(context, errors), new ArrayList<Binding>());

      if (typedDef instanceof FunctionDefinition) {
        FunctionDefinition funcDef = (FunctionDefinition) typedDef;
        if (funcDef.getTerm() != null && !(funcDef.getTerm() instanceof ElimExpression)) {
          typedDef = new FunctionDefinition(funcDef.getName(), funcDef.getPrecedence(), funcDef.getFixity(), funcDef.getArguments(), funcDef.getResultType().normalize(NormalizeVisitor.Mode.NF), funcDef.getArrow(), funcDef.getTerm().normalize(NormalizeVisitor.Mode.NF));
        }
      }
    }

    for (TypeCheckingError error : errors) {
      System.err.print(fileName + ": ");
      if (error.getExpression() instanceof Concrete.SourceNode) {
        Concrete.Position position = ((Concrete.SourceNode) error.getExpression()).getPosition();
        System.err.print(position.line + ":" + position.column + ": ");
      }
      System.err.println(error);
    }

    if (errors.isEmpty()) {
      System.out.println("[OK] " + fileName);
    }
  }
}
