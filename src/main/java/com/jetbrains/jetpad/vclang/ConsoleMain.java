package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.module.output.FileOutputSupplier;
import com.jetbrains.jetpad.vclang.module.source.FileSourceSupplier;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ValidateTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingOrdering;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.definition.BaseDefinition.Helper.toNamespaceMember;

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

    String sourceDirStr = cmdLine.getOptionValue("s");
    final File sourceDir = new File(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);
    String outputDirStr = cmdLine.getOptionValue("o");
    File outputDir = outputDirStr == null ? sourceDir : new File(outputDirStr);
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

    Root.initialize();
    final List<ModuleID> loadedModules = new ArrayList<>();
    final List<Abstract.Definition> modulesToTypeCheck = new ArrayList<>();
    final BaseModuleLoader moduleLoader = new BaseModuleLoader(recompile) {
      @Override
      public void savingError(GeneralError error) {
        System.err.println(error);
      }

      @Override
      public void loadingError(GeneralError error) {
        System.err.println(error);
      }

      @Override
      public void loadingSucceeded(ModuleID module, NamespaceMember nsMember, boolean compiled) {
        if (nsMember.abstractDefinition != null) {
          modulesToTypeCheck.add(nsMember.abstractDefinition);
        }
        if (compiled) {
          loadedModules.add(module);
          System.out.println("[Resolved] " + module.getModulePath());
        } else {
          System.out.println("[Loaded] " + module.getModulePath());
        }
      }
    };

    final ListErrorReporter errorReporter = new ListErrorReporter();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    moduleLoader.setSourceSupplier(new FileSourceSupplier(moduleLoader, errorReporter, sourceDir));
    moduleLoader.setOutputSupplier(new FileOutputSupplier(moduleDeserialization, outputDir, libDirs));

    if (cmdLine.getArgList().isEmpty()) {
      if (sourceDirStr == null) return;
      try {
        Files.walkFileTree(sourceDir.toPath(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            if (path.getFileName().toString().endsWith(FileOperations.EXTENSION)) {
              processFile(moduleLoader, errorReporter, path, sourceDir);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            System.err.println(GeneralError.ioError(e));
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        System.err.println(GeneralError.ioError(e));
      }
    } else {
      for (String fileName : cmdLine.getArgList()) {
        processFile(moduleLoader, errorReporter, Paths.get(fileName), sourceDir);
      }
    }

    final Set<ModuleID> failedModules = new HashSet<>();

    TypecheckingOrdering.typecheck(modulesToTypeCheck, errorReporter, new TypecheckedReporter() {
      @Override
      public void typecheckingSucceeded(Abstract.Definition abstractDefinition, Definition definition) {
        if (definition instanceof FunctionDefinition) {
          ValidateTypeVisitor visitor = new ValidateTypeVisitor();
          ((FunctionDefinition) definition).getElimTree().accept(visitor, ((FunctionDefinition) definition).getResultType());
          if (visitor.myErrorReporter.errors() > 0) {
            System.err.println("Checking " + definition.getName());
            System.err.println(((FunctionDefinition) definition).getElimTree());
            System.err.println(visitor.myErrorReporter);
          }
        }
      }

      @Override
      public void typecheckingFailed(Abstract.Definition definition) {
        failedModules.add(toNamespaceMember(definition).getResolvedName().getModuleID());
        for (GeneralError error : errorReporter.getErrorList()) {
          System.err.println(error);
        }
        errorReporter.getErrorList().clear();
      }
    });

    for (ModuleID moduleID : failedModules) {
      if (failedModules.contains(moduleID)) {
        System.out.println("[FAILED] " + moduleID.getModulePath());
      } else {
        System.out.println("[OK] " + moduleID.getModulePath());
      }
    }

    for (GeneralError error : errorReporter.getErrorList()) {
      System.err.println(error);
    }

    for (ModuleID moduleID : loadedModules) {
      moduleLoader.save(moduleID);
    }
  }

  static private ModulePath getModule(Path file) {
    int nameCount = file.getNameCount();
    if (nameCount < 1) return null;
    List<String> names = new ArrayList<>(nameCount);
    for (int i = 0; i < nameCount; ++i) {
      String name = file.getName(i).toString();
      if (i == nameCount - 1) {
        if (!name.endsWith(FileOperations.EXTENSION)) return null;
        name = name.substring(0, name.length() - 3);
      }

      if (name.length() == 0 || !(Character.isLetterOrDigit(name.charAt(0)) || name.charAt(0) == '_')) return null;
      for (int j = 1; j < name.length(); ++j) {
        if (!(Character.isLetterOrDigit(name.charAt(j)) || name.charAt(j) == '_' || name.charAt(j) == '-' || name.charAt(j) == '\'')) return null;
      }
      names.add(name);
    }
    return new ModulePath(names);
  }

  static private void processFile(ModuleLoader moduleLoader, ListErrorReporter errorReporter, Path fileName, File sourceDir) {
    Path relativePath = sourceDir != null && fileName.startsWith(sourceDir.toPath()) ? sourceDir.toPath().relativize(fileName) : fileName;
    ModulePath modulePath = getModule(relativePath);
    if (modulePath == null) {
      System.err.println(fileName + ": incorrect file name");
      return;
    }

    ModuleID moduleID = moduleLoader.locateModule(modulePath);
    if (moduleID == null) {
      throw new IllegalStateException();
    }
    moduleLoader.load(moduleID);

    for (GeneralError error : errorReporter.getErrorList()) {
      System.err.println(error);
    }

    errorReporter.getErrorList().clear();
  }
}
