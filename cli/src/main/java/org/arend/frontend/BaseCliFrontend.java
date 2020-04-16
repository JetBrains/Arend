package org.arend.frontend;

import org.apache.commons.cli.*;
import org.arend.core.definition.Definition;
import org.arend.error.ListErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.extImpl.DefinitionRequester;
import org.arend.frontend.library.FileSourceLibrary;
import org.arend.library.*;
import org.arend.library.error.LibraryError;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.prelude.Prelude;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.term.group.Group;
import org.arend.term.prettyprint.PrettyPrinterConfigWithRenamer;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.doubleChecker.CoreModuleChecker;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.util.FileUtils;
import org.arend.util.Range;

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
  private final Map<ModulePath, GeneralError.Level> myModuleResults = new LinkedHashMap<>();

  // Libraries
  private final FileLibraryResolver myLibraryResolver = new FileLibraryResolver(new ArrayList<>(), myTypecheckerState, System.err::println);
  private final LibraryManager myLibraryManager = new MyLibraryManager();

  private static String timeToString(long time) {
    if (time < 10000) {
      return time + "ms";
    }
    if (time < 60000) {
      return time / 1000 + ("." + (time / 100 % 10)) + "s";
    }

    long seconds = time / 1000;
    return (seconds / 60) + "m" + (seconds % 60) + "s";
  }

  private class MyLibraryManager extends LibraryManager {
    private final Stack<Long> times = new Stack<>();

    MyLibraryManager() {
      super(myLibraryResolver, new InstanceProviderSet(), myErrorReporter, System.err::println, DefinitionRequester.INSTANCE);
    }

    @Override
    protected void beforeLibraryLoading(Library library) {
      System.out.println("[INFO] Loading library " + library.getName());
      times.push(System.currentTimeMillis());
    }

    @Override
    protected void afterLibraryLoading(Library library, boolean successful) {
      long time = System.currentTimeMillis() - times.pop();
      flushErrors();
      System.err.flush();
      System.out.println("[INFO] " + (successful ? "Loaded " : "Failed loading ") + "library " + library.getName() + (successful ? " (" + timeToString(time) + ")" : ""));
    }
  }

  public LibraryManager getLibraryManager() {
    return myLibraryManager;
  }

  public TypecheckerState getTypecheckerState() {
    return myTypecheckerState;
  }


  private class MyTypechecking extends TypecheckingOrderingListener {
    private int total;
    private int failed;

    MyTypechecking() {
      super(myLibraryManager.getInstanceProviderSet(), myTypecheckerState, ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, myErrorReporter, PositionComparator.INSTANCE);
    }

    @Override
    public void typecheckingBodyFinished(TCReferable referable, Definition definition) {
      update(definition);
    }

    @Override
    public void typecheckingUnitFinished(TCReferable referable, Definition definition) {
      update(definition);
    }

    private void update(Definition definition) {
      flushErrors();

      total++;
      if (definition.status().hasErrors()) {
        failed++;
      }
    }

    private void clear() {
      total = 0;
      failed = 0;
    }
  }

  private CommandLine parseArgs(String[] args) {
    try {
      Options cmdOptions = new Options();
      cmdOptions.addOption("h", "help", false, "print this message");
      cmdOptions.addOption(Option.builder("L").longOpt("libdir").hasArg().argName("dir").desc("directory containing libraries").build());
      cmdOptions.addOption(Option.builder("l").longOpt("lib").hasArg().argName("library").desc("project dependency (a name of a library or a path to it)").build());
      cmdOptions.addOption(Option.builder("s").longOpt("sources").hasArg().argName("dir").desc("project source directory").build());
      cmdOptions.addOption(Option.builder("b").longOpt("binaries").hasArg().argName("dir").desc("project output directory").build());
      cmdOptions.addOption(Option.builder("e").longOpt("extensions").hasArg().argName("dir").desc("language extensions directory").build());
      cmdOptions.addOption(Option.builder("m").longOpt("extension-main").hasArg().argName("class").desc("main extension class").build());
      cmdOptions.addOption(Option.builder("r").longOpt("recompile").desc("recompile files").build());
      cmdOptions.addOption(Option.builder("c").longOpt("double-check").desc("double check correctness of the result").build());
      cmdOptions.addOption("t", "test", false, "run tests");
      cmdOptions.addOption("v", "version", false, "print language version");
      addCommandOptions(cmdOptions);
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        new HelpFormatter().printHelp("arend [FILES]", cmdOptions);
        return null;
      }

      if (cmdLine.hasOption("v")) {
        System.out.println("Arend " + Prelude.VERSION);
        return null;
      }

      return cmdLine;
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

    if (!myLibraryManager.loadLibrary(new PreludeResourceLibrary(myTypecheckerState), null)) {
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

    String binaryDirStr = cmdLine.getOptionValue("b");
    Path outDir = binaryDirStr != null ? Paths.get(binaryDirStr) : sourceDir.resolve(".bin");

    String extDirStr = cmdLine.getOptionValue("e");
    Path extDir = extDirStr != null ? Paths.get(extDirStr) : null;
    String extMainClass = cmdLine.getOptionValue("m");

    // Collect modules and libraries for which typechecking was requested
    Collection<String> argFiles = cmdLine.getArgList();
    Set<ModulePath> requestedModules;
    List<UnmodifiableSourceLibrary> requestedLibraries = new ArrayList<>();
    if (argFiles.isEmpty()) {
      if (sourceDirStr != null) {
        requestedModules = new LinkedHashSet<>();
        FileUtils.getModules(sourceDir, FileUtils.EXTENSION, requestedModules, myLibraryManager.getLibraryErrorReporter());
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
            myLibraryManager.getLibraryErrorReporter().report(FileUtils.illegalModuleName(fileName));
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
      requestedLibraries.add(new FileSourceLibrary("\\default", sourceDir, outDir, extDir, extMainClass, requestedModules, argFiles.isEmpty(), libraryDependencies, Range.unbound(), myTypecheckerState));
    }

    if (requestedLibraries.isEmpty()) {
      Path path = Paths.get(FileUtils.LIBRARY_CONFIG_FILE);
      if (Files.isRegularFile(path)) {
        UnmodifiableSourceLibrary library = myLibraryResolver.registerLibrary(path.toAbsolutePath());
        if (library != null) {
          requestedLibraries.add(library);
        }
      } else {
        System.out.println("Nothing to load");
        return cmdLine;
      }
    }

    // Load and typecheck libraries
    MyTypechecking typechecking = new MyTypechecking();
    boolean recompile = cmdLine.hasOption("r");
    boolean doubleCheck = cmdLine.hasOption("c");
    for (UnmodifiableSourceLibrary library : requestedLibraries) {
      myModuleResults.clear();
      if (recompile) {
        library.addFlag(SourceLibrary.Flag.RECOMPILE);
      }
      if (!myLibraryManager.loadLibrary(library, typechecking)) {
        continue;
      }

      Collection<? extends ModulePath> modules = library.getUpdatedModules();
      if (modules.isEmpty()) {
        continue;
      }

      System.out.println();
      System.out.println("--- Typechecking " + library.getName() + " ---");
      long time = System.currentTimeMillis();
      typechecking.typecheckLibrary(library);
      time = System.currentTimeMillis() - time;
      flushErrors();

      // Output nice per-module typechecking results
      int numWithErrors = 0;
      int numWithGoals = 0;
      for (ModulePath module : modules) {
        GeneralError.Level result = myModuleResults.get(module);
        if (result == null && library.getModuleGroup(module) == null) {
          result = GeneralError.Level.ERROR;
        }
        reportTypeCheckResult(module, result);
        if (result == GeneralError.Level.ERROR) numWithErrors++;
        if (result == GeneralError.Level.GOAL) numWithGoals++;
      }

      if (numWithErrors > 0) {
        System.out.println("Number of modules with errors: " + numWithErrors);
      }
      if (numWithGoals > 0) {
        System.out.println("Number of modules with goals: " + numWithGoals);
      }
      System.out.println("--- Done (" + timeToString(time) + ") ---");

      // Persist updated modules
      if (library.supportsPersisting()) {
        library.persistUpdatedModules(System.err::println);
        library.clearUpdateModules();
      }

      if (doubleCheck && numWithErrors == 0) {
        System.out.println();
        System.out.println("--- Checking " + library.getName() + " ---");
        time = System.currentTimeMillis();

        CoreModuleChecker checker = new CoreModuleChecker(myErrorReporter, myTypecheckerState);
        for (ModulePath module : library.getLoadedModules()) {
          Group group = library.getModuleGroup(module);
          if (group != null) {
            checker.checkGroup(group);
          }
        }

        time = System.currentTimeMillis() - time;
        flushErrors();
        System.out.println("--- Done (" + timeToString(time) + ") ---");
      }
    }

    // Run tests
    if (cmdLine.hasOption("t")) {
      for (UnmodifiableSourceLibrary library : requestedLibraries) {
        Collection<? extends ModulePath> modules = library.getTestModules();
        if (modules.isEmpty()) {
          continue;
        }

        System.out.println("[INFO] Loading tests for " + library.getName());
        long time = System.currentTimeMillis();
        boolean loaded = library.loadTests(myLibraryManager);
        time = System.currentTimeMillis() - time;
        if (!loaded) {
          System.out.println("[INFO] Failed loading tests for " + library.getName());
          continue;
        }
        System.out.println("[INFO] Loaded tests for " + library.getName() + " (" + timeToString(time) + ")");

        System.out.println();
        System.out.println("--- Running tests in " + library.getName() + " ---");
        typechecking.clear();
        time = System.currentTimeMillis();
        typechecking.typecheckTests(library, null);
        time = System.currentTimeMillis() - time;
        flushErrors();
        System.out.println("Tests completed: " + typechecking.total + ", Failed: " + typechecking.failed);
        System.out.println("--- Done (" + timeToString(time) + ") ---");
      }
    }

    return cmdLine;
  }

  private void flushErrors() {
    for (GeneralError error : myErrorReporter.getErrorList()) {
      error.forAffectedDefinitions((referable, err) -> {
        if (referable instanceof LocatedReferable) {
          updateSourceResult(((LocatedReferable) referable).getLocation(), err.level);
        }
      });

      //Print error
      PrettyPrinterConfigWithRenamer ppConfig = new PrettyPrinterConfigWithRenamer();
      if (error instanceof GoalError) {
        ppConfig.expressionFlags = EnumSet.of(PrettyPrinterFlag.SHOW_FIELD_INSTANCE);
      }
      String errorText = error.getDoc(ppConfig).toString();

      if (error.isSevere()) {
        System.err.println(errorText);
        System.err.flush();
      } else {
        System.out.println(errorText);
      }
    }
    myErrorReporter.getErrorList().clear();
  }

  private void updateSourceResult(ModulePath module, GeneralError.Level result) {
    GeneralError.Level prevResult = myModuleResults.get(module);
    if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
      myModuleResults.put(module, result);
    }
  }

  private void reportTypeCheckResult(ModulePath modulePath, GeneralError.Level result) {
    System.out.println("[" + resultChar(result) + "]" + " " + modulePath);
  }

  private static char resultChar(GeneralError.Level result) {
    if (result == null) {
      return ' ';
    }
    switch (result) {
      case GOAL:
        return '\u25ef';
      case ERROR:
        return '\u2717';
      default:
        return '\u00b7';
    }
  }
}
