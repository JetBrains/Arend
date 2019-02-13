package org.arend.frontend;

import org.apache.commons.cli.*;
import org.arend.core.definition.Definition;
import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.error.ListErrorReporter;
import org.arend.library.*;
import org.arend.library.error.LibraryError;
import org.arend.module.ModulePath;
import org.arend.module.error.ExceptionError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.util.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class BaseCliFrontend {
  // Typechecking
  private final TypecheckerState myTypecheckerState = new SimpleTypecheckerState();
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();
  private final Map<ModulePath, Error.Level> myModuleResults = new LinkedHashMap<>();

  // Libraries
  private final FileLibraryResolver myLibraryResolver = new FileLibraryResolver(new ArrayList<>(), myTypecheckerState, System.err::println);
  private final LibraryManager myLibraryManager = new MyLibraryManager();

  private class MyLibraryManager extends LibraryManager {
    MyLibraryManager() {
      super(myLibraryResolver, new InstanceProviderSet(), myErrorReporter, System.err::println);
    }

    @Override
    protected void beforeLibraryLoading(Library library) {
      System.out.println("[INFO] Loading library " + library.getName());
    }

    @Override
    protected void afterLibraryLoading(Library library, boolean successful) {
      flushErrors();
      System.err.flush();
      System.out.println("[INFO] " + (successful ? "Loaded " : "Failed loading ") + "library " + library.getName());
    }
  }

  public LibraryManager getLibraryManager() {
    return myLibraryManager;
  }

  public TypecheckerState getTypecheckerState() {
    return myTypecheckerState;
  }


  private class MyTypechecking extends TypecheckingOrderingListener {
    MyTypechecking() {
      super(myLibraryManager.getInstanceProviderSet(), myTypecheckerState, ConcreteReferableProvider.INSTANCE, myErrorReporter, PositionComparator.INSTANCE);
    }

    @Override
    public void typecheckingBodyFinished(TCReferable referable, Definition definition) {
      flushErrors();
    }

    @Override
    public void typecheckingUnitFinished(TCReferable referable, Definition definition) {
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
        new HelpFormatter().printHelp("arend [FILES]", cmdOptions);
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

  public CommandLine run(String[] args) {
    CommandLine cmdLine = parseArgs(args);
    if (cmdLine == null) {
      return null;
    }

    if (!myLibraryManager.loadLibrary(new PreludeResourceLibrary(myTypecheckerState))) {
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
    if (libDirStrings != null && libStrings != null) {
      for (String libString : libStrings) {
        if (FileUtils.isLibraryName(libString)) {
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
    Path outDir = binaryDirStr != null ? Paths.get(binaryDirStr) : sourceDir.resolve(".bin");

    // Collect modules and libraries for which typechecking was requested
    Collection<String> argFiles = cmdLine.getArgList();
    Set<ModulePath> requestedModules;
    List<UnmodifiableSourceLibrary> requestedLibraries = new ArrayList<>();
    if (argFiles.isEmpty()) {
      if (sourceDirStr != null) {
        requestedModules = new LinkedHashSet<>();
        FileUtils.getModules(sourceDir, FileUtils.EXTENSION, requestedModules);
      } else {
        requestedModules = Collections.emptySet();
      }
    } else {
      requestedModules = new LinkedHashSet<>();
      for (String fileName : argFiles) {
        boolean isPath = fileName.contains(FileSystems.getDefault().getSeparator());
        Path path = Paths.get(fileName);
        if (fileName.endsWith(FileUtils.LIBRARY_CONFIG_FILE) || isPath && Files.isDirectory(path)) {
          UnmodifiableSourceLibrary library = myLibraryResolver.registerLibrary(path.toAbsolutePath());
          if (library != null) {
            requestedLibraries.add(library);
          }
        } else {
          ModulePath modulePath;
          if (isPath || fileName.endsWith(FileUtils.EXTENSION)) {
            modulePath = path.isAbsolute() ? null : FileUtils.modulePath(path, FileUtils.EXTENSION);
          } else {
            modulePath = FileUtils.modulePath(fileName);
          }
          if (modulePath == null) {
            FileUtils.printIllegalModuleName(fileName);
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
      requestedLibraries.add(new FileSourceLibrary("\\default", sourceDir, outDir, requestedModules, argFiles.isEmpty(), libraryDependencies, myTypecheckerState));
    }

    // Load and typecheck libraries
    if (requestedLibraries.isEmpty()) {
      System.out.println("Nothing to load");
      return cmdLine;
    }

    boolean recompile = cmdLine.hasOption("recompile");
    for (UnmodifiableSourceLibrary library : requestedLibraries) {
      myModuleResults.clear();
      if (recompile) {
        library.addFlag(SourceLibrary.Flag.RECOMPILE);
      }
      if (!myLibraryManager.loadLibrary(library)) {
        continue;
      }

      if (!library.needsTypechecking()) {
        continue;
      }

      System.out.println("--- Typechecking " + library.getName() + " ---");
      Collection<? extends ModulePath> modules = library.getUpdatedModules();
      new MyTypechecking().typecheckLibrary(library);
      flushErrors();

      // Output nice per-module typechecking results
      int numWithErrors = 0;
      int numWithGoals = 0;
      for (ModulePath module : modules) {
        Error.Level result = myModuleResults.get(module);
        if (result == null && library.getModuleGroup(module) == null) {
          result = Error.Level.ERROR;
        }
        reportTypeCheckResult(module, result);
        if (result == Error.Level.ERROR) numWithErrors++;
        if (result == Error.Level.GOAL) numWithGoals++;
      }

      if (numWithErrors > 0) {
        System.out.println("Number of modules with errors: " + numWithErrors);
      }
      if (numWithGoals > 0) {
        System.out.println("Number of modules with goals: " + numWithGoals);
      }
      System.out.println("--- Done ---");

      // Persist updated modules
      if (library.supportsPersisting()) {
        library.persistUpdateModules(System.err::println);
        library.clearUpdateModules();
      }
    }

    return cmdLine;
  }

  private void flushErrors() {
    for (GeneralError error : myErrorReporter.getErrorList()) {
      for (GlobalReferable referable : error.getAffectedDefinitions()) {
        if (referable instanceof LocatedReferable) {
          updateSourceResult(((LocatedReferable) referable).getLocation(), error.level);
        }
      }

      if (error instanceof ExceptionError || error.getAffectedDefinitions().isEmpty()) {
        System.err.println(error);
        System.err.flush();
      } else {
        System.out.println(error);
      }
    }
    myErrorReporter.getErrorList().clear();
  }

  private void updateSourceResult(ModulePath module, Error.Level result) {
    Error.Level prevResult = myModuleResults.get(module);
    if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
      myModuleResults.put(module, result);
    }
  }

  private void reportTypeCheckResult(ModulePath modulePath, Error.Level result) {
    StringBuilder builder = new StringBuilder();
    builder.append("[").append(resultChar(result)).append("]");
    builder.append(" ").append(modulePath);
    System.out.println(builder);
  }

  private static char resultChar(Error.Level result) {
    if (result == null) {
      return ' ';
    }
    switch (result) {
      case GOAL:
        return '◯';
      case ERROR:
        return '✗';
      default:
        return '·';
    }
  }
}
