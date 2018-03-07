package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.library.*;
import com.jetbrains.jetpad.vclang.library.error.LibraryError;
import com.jetbrains.jetpad.vclang.library.resolver.LibraryResolver;
import com.jetbrains.jetpad.vclang.library.resolver.SearchingModuleLocator;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.CachingModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.EmptyModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.LocatingModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.prelude.PreludeResourceLibrary;
import com.jetbrains.jetpad.vclang.typechecking.SimpleTypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.util.FileUtils;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class BaseCliFrontend {
  // Typechecking
  private final TypecheckerState myTypecheckerState = new SimpleTypecheckerState();
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();
  private final Map<ModulePath, ModuleResult> myModuleResults = new LinkedHashMap<>();

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  // Libraries
  private final FileLibraryResolver myLibraryResolver = new FileLibraryResolver(new ArrayList<>(), myTypecheckerState, System.err::println);
  private final LibraryManager myLibraryManager = new MyLibraryManager(myLibraryResolver, EmptyModuleScopeProvider.INSTANCE, System.err::println);
  private final CachingModuleScopeProvider myModuleScopeProvider = new CachingModuleScopeProvider(new LocatingModuleScopeProvider(new SearchingModuleLocator(myLibraryManager)));

  private static class MyLibraryManager extends LibraryManager {
    MyLibraryManager(LibraryResolver libraryResolver, ModuleScopeProvider moduleScopeProvider, ErrorReporter errorReporter) {
      super(libraryResolver, moduleScopeProvider, errorReporter);
    }

    @Override
    protected void beforeLibraryLoading(Library library) {
      System.out.println("--- Loading " + library.getName() + " ---");
    }

    @Override
    protected void afterLibraryLoading(Library library, boolean successful) {
      System.out.println((successful ? "[LOADED] " : "[FAILED] ") + library.getName());
    }
  }

  public BaseCliFrontend() {
    myLibraryManager.setModuleScopeProvider(myModuleScopeProvider);
  }

  public LibraryManager getLibraryManager() {
    return myLibraryManager;
  }

  public CachingModuleScopeProvider getModuleScopeProvider() {
    return myModuleScopeProvider;
  }

  public TypecheckerState getTypecheckerState() {
    return myTypecheckerState;
  }


  private class MyTypechecking extends Typechecking {
    MyTypechecking() {
      super(myTypecheckerState, ConcreteReferableProvider.INSTANCE, myErrorReporter);
    }

    @Override
    public void typecheckingFinished(GlobalReferable referable, Definition definition) {
      flushErrors();
    }
  }

  private CommandLine parseArgs(String[] args) {
    try {
      Options cmdOptions = new Options();
      cmdOptions.addOption("h", "help", false, "print this message");
      cmdOptions.addOption(Option.builder("L").longOpt("libdir").hasArg().argName("dir").desc("directory containing libraries").build());
      cmdOptions.addOption(Option.builder("l").longOpt("lib").hasArg().argName("library").desc("project dependency (a name of a library or a path to it)").build());
      cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("srcdir").desc("project source directory").build());
      cmdOptions.addOption(Option.builder("o").longOpt("output").hasArg().argName("outdir").desc("project output directory").build());
      cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
      addCommandOptions(cmdOptions);
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
        return null;
      } else {
        return cmdLine;
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      return null;
    }
  }

  protected void addCommandOptions(Options cmdOptions) {}

  private static void printIllegalModuleName(String module) {
    System.err.println("[ERROR] " + module + " is an illegal module path");
  }

  public CommandLine run(String[] args) {
    if (!myLibraryManager.loadLibrary(new PreludeResourceLibrary(myTypecheckerState))) {
      return null;
    }

    CommandLine cmdLine = parseArgs(args);
    if (cmdLine == null) {
      return null;
    }

    // Get library directories
    String[] libDirStrings = cmdLine.getOptionValues("L");
    if (libDirStrings != null) {
      for (String libDirString : libDirStrings) {
        Path libDir = Paths.get(libDirString);
        if (Files.isDirectory(libDir)) {
          myLibraryResolver.addLibraryDirectory(libDir);
        } else {
          System.err.println("[ERROR] " + libDir + " is not a directory");
        }
      }
    }

    // Get library dependencies
    String[] libStrings = cmdLine.getOptionValues("l");
    List<LibraryDependency> libraryDependencies = new ArrayList<>();
    if (libDirStrings != null) {
      for (String libString : libStrings) {
        if (libString.endsWith(FileUtils.LIBRARY_EXTENSION)) {
          Library library = myLibraryResolver.registerLibrary(Paths.get(libString));
          if (library != null) {
            libraryDependencies.add(new LibraryDependency(library.getName()));
          }
        } else if (FileUtils.isLibraryName(libString)) {
          libraryDependencies.add(new LibraryDependency(libString));
        } else {
          System.err.println(LibraryError.illegalName(libString));
        }
      }
    }

    // Get source and output directories
    String sourceDirStr = cmdLine.getOptionValue("s");
    Path sourceDir = sourceDirStr == null ? FileUtils.getCurrentDirectory() : Paths.get(sourceDirStr);

    String binaryDirStr = cmdLine.getOptionValue("o");
    Path outDir = binaryDirStr != null ? Paths.get(binaryDirStr) : sourceDir.resolve(".output");

    // Collect modules and libraries for which typechecking was requested
    Collection<String> argFiles = cmdLine.getArgList();
    List<ModulePath> requestedModules = new ArrayList<>();
    List<SourceLibrary> requestedLibraries = new ArrayList<>();
    if (argFiles.isEmpty()) {
      if (sourceDirStr != null) {
        try {
          Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
              if (path.getFileName().toString().endsWith(FileUtils.EXTENSION)) {
                path = sourceDir.relativize(path);
                ModulePath modulePath = FileUtils.modulePath(path, FileUtils.EXTENSION);
                if (modulePath == null) {
                  printIllegalModuleName(path.toString());
                } else {
                  requestedModules.add(modulePath);
                }
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) {
              System.err.println(e.getMessage());
              return FileVisitResult.CONTINUE;
            }
          });
        } catch (IOException e) {
          System.err.println(e.getMessage());
        }
      }
    } else {
      for (String fileName : argFiles) {
        if (fileName.endsWith(FileUtils.LIBRARY_EXTENSION)) {
          SourceLibrary library = myLibraryResolver.registerLibrary(Paths.get(fileName));
          if (library != null) {
            requestedLibraries.add(library);
          }
        } else {
          ModulePath modulePath;
          if (fileName.endsWith(FileUtils.EXTENSION)) {
            modulePath = FileUtils.modulePath(Paths.get(fileName), FileUtils.EXTENSION);
          } else {
            modulePath = FileUtils.modulePath(fileName);
          }
          if (modulePath == null) {
            printIllegalModuleName(fileName);
          } else {
            requestedModules.add(modulePath);
          }
        }
      }
    }
    if (!requestedModules.isEmpty()) {
      try {
        Files.createDirectories(outDir);
      } catch (IOException e) {
        e.printStackTrace();
        outDir = null;
      }
      requestedLibraries.add(new FileSourceLibrary("\\default", sourceDir, outDir, requestedModules, libraryDependencies, myTypecheckerState));
    }

    // Load and typecheck libraries
    if (requestedLibraries.isEmpty()) {
      System.out.println("Nothing to load");
      return cmdLine;
    }

    boolean recompile = cmdLine.hasOption("recompile");
    for (SourceLibrary library : requestedLibraries) {
      if (recompile) {
        library.addFlag(SourceLibrary.Flag.RECOMPILE);
      }
      myLibraryManager.loadLibrary(library);

      if (!library.needsTypechecking()) {
        continue;
      }

      System.out.println("--- Typechecking " + library.getName() + " ---");
      library.typecheck(new MyTypechecking(), myErrorReporter);
      flushErrors();

      // Output nice per-module typechecking results
      int numWithErrors = 0;
      for (Map.Entry<ModulePath, ModuleResult> entry : myModuleResults.entrySet()) {
        if (!requestedModules.contains(entry.getKey())) {
          ModuleResult result = entry.getValue();
          reportTypeCheckResult(entry.getKey(), result == ModuleResult.OK ? ModuleResult.UNKNOWN : result);
          if (result == ModuleResult.ERRORS) numWithErrors += 1;
        }
      }

      if (numWithErrors > 0) {
        System.out.println("Number of modules with errors: " + numWithErrors);
      }
      System.out.println("--- Done ---");
    }

    return cmdLine;
  }

  private void flushErrors() {
    for (GeneralError error : myErrorReporter.getErrorList()) {
      ModuleResult moduleResult = error.level == Error.Level.ERROR ? ModuleResult.ERRORS : error.level == Error.Level.GOAL ? ModuleResult.GOALS : null;
      if (moduleResult != null) {
        for (GlobalReferable referable : error.getAffectedDefinitions()) {
          if (referable instanceof LocatedReferable) {
            updateSourceResult(((LocatedReferable) referable).getLocation(null), moduleResult);
          }
        }
      }

      System.out.println(error);
    }
    myErrorReporter.getErrorList().clear();
  }

  private void updateSourceResult(ModulePath module, ModuleResult result) {
    ModuleResult prevResult = myModuleResults.get(module);
    if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
      myModuleResults.put(module, result);
    }
  }

  private void reportTypeCheckResult(ModulePath modulePath, ModuleResult result) {
    StringBuilder builder = new StringBuilder();
    builder.append("[").append(resultChar(result)).append("]");
    builder.append(" ").append(modulePath);
    System.out.println(builder);
  }

  private static char resultChar(ModuleResult result) {
    switch (result) {
      case NOT_LOADED:
        return '✗';
      case OK:
        return ' ';
      case GOALS:
        return '◯';
      case ERRORS:
        return '✗';
      default:
        return '·';
    }
  }
}
