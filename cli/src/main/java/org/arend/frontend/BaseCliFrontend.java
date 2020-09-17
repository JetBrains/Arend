package org.arend.frontend;

import org.apache.commons.cli.*;
import org.arend.core.definition.Definition;
import org.arend.ext.error.ListErrorReporter;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.extImpl.DefinitionRequester;
import org.arend.frontend.library.FileSourceLibrary;
import org.arend.frontend.library.TimedLibraryManager;
import org.arend.frontend.repl.PlainCliRepl;
import org.arend.frontend.repl.jline.JLineCliRepl;
import org.arend.library.*;
import org.arend.library.classLoader.FileClassLoaderDelegate;
import org.arend.library.error.LibraryError;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.scope.EmptyScope;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.term.prettyprint.PrettyPrinterConfigWithRenamer;
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.doubleChecker.CoreModuleChecker;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.util.FileUtils;
import org.arend.util.Range;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.arend.frontend.library.TimedLibraryManager.timeToString;

public abstract class BaseCliFrontend {
  // Typechecking
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();
  private final Map<ModulePath, GeneralError.Level> myModuleResults = new LinkedHashMap<>();

  // Status information
  private boolean myExitWithError = false;
  private final ErrorReporter mySystemErrErrorReporter = error -> {
    System.err.println(error);
    myExitWithError = true;
  };

  // Libraries
  private final FileLibraryResolver myLibraryResolver = new FileLibraryResolver(new ArrayList<>(), mySystemErrErrorReporter);
  private final LibraryManager myLibraryManager = new TimedLibraryManager(myLibraryResolver, new InstanceProviderSet(), myErrorReporter, mySystemErrErrorReporter, DefinitionRequester.INSTANCE) {
    @Override
    protected void afterLibraryLoading(@NotNull Library library, boolean successful) {
      super.afterLibraryLoading(library, successful);
      flushErrors();
    }
  };

  public LibraryManager getLibraryManager() {
    return myLibraryManager;
  }


  private class MyTypechecking extends TypecheckingOrderingListener {
    private int total;
    private int failed;

    MyTypechecking() {
      super(myLibraryManager.getInstanceProviderSet(), ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, myErrorReporter, PositionComparator.INSTANCE, new LibraryArendExtensionProvider(myLibraryManager));
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

      LocatedReferable parent = definition.getRef().getLocatedReferableParent();
      if (parent == null || parent instanceof ModuleReferable) {
        total++;
        if (definition.status().hasErrors()) {
          failed++;
        }
      }
    }

    private void clear() {
      total = 0;
      failed = 0;
    }
  }

  public boolean isExitWithError() {
    return myExitWithError;
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
      cmdOptions.addOption(Option.builder("r").longOpt("recompile").hasArg().optionalArg(true).argName("target").desc("recompile files").build());
      cmdOptions.addOption(Option.builder("c").longOpt("double-check").desc("double check correctness of the result").build());
      cmdOptions.addOption(Option.builder("i").longOpt("interactive").hasArg().optionalArg(true).argName("type").desc("start an interactive REPL, type can be plain or jline (default)").build());
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
      myExitWithError = true;
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

    var replKind = cmdLine.getOptionValue("i", "jline");
    var libDirStrings = cmdLine.hasOption("L")
        ? cmdLine.getOptionValues("L")
        : new String[] { FileUtils.defaultLibrariesRoot().toString() };

    // Get library directories
    var libDirs = new ArrayList<Path>(libDirStrings.length);
    for (String libDirString : libDirStrings) {
      var libDir = Paths.get(libDirString);
      if (Files.isDirectory(libDir)) {
        libDirs.add(libDir);
      } else {
        myExitWithError = true;
        System.err.println("[ERROR] " + libDir + " is not a directory");
      }
    }

    String recompileString = cmdLine.getOptionValue("r");
    ModulePath recompileModule = null;
    LongName recompileDef = null;
    if (recompileString != null) {
      int index = recompileString.indexOf(':');
      if (index >= 0) {
        recompileDef = LongName.fromString(recompileString.substring(index + 1));
        if (!FileUtils.isCorrectDefinitionName(recompileDef)) {
          System.err.println(FileUtils.illegalDefinitionName(recompileDef.toString()));
          recompileDef = null;
        }
        recompileString = recompileString.substring(0, index);
      }
      recompileModule = ModulePath.fromString(recompileString);
      if (!FileUtils.isCorrectModulePath(recompileModule)) {
        System.err.println(FileUtils.illegalModuleName(recompileModule.toString()));
        recompileModule = null;
      }
    }

    boolean recompile = recompileString == null && cmdLine.hasOption("r");
    if (cmdLine.hasOption("i")) {
      switch (replKind.toLowerCase()) {
        default:
          System.err.println("[ERROR] Unrecognized repl type: " + replKind);
          break;
        case "plain":
          PlainCliRepl.launch(recompile, libDirs);
          break;
        case "jline":
          JLineCliRepl.launch(recompile, libDirs);
          break;
      }
      return null;
    }

    if (!myLibraryManager.loadLibrary(new PreludeResourceLibrary(), null)) {
      return null;
    }

    myLibraryResolver.addLibraryDirectories(libDirs);

    // Get library dependencies
    String[] libStrings = cmdLine.getOptionValues("l");
    List<LibraryDependency> libraryDependencies = new ArrayList<>();
    if (libStrings != null) {
      for (String libString : libStrings) {
        if (FileUtils.isLibraryName(libString)) {
          libraryDependencies.add(new LibraryDependency(libString));
        } else {
          myExitWithError = true;
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
    List<SourceLibrary> requestedLibraries = new ArrayList<>();
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
          SourceLibrary library = myLibraryResolver.registerLibrary(path.toAbsolutePath().normalize());
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
      requestedLibraries.add(new FileSourceLibrary("\\default", sourceDir, outDir, new LibraryHeader(requestedModules, libraryDependencies, Range.unbound(), new FileClassLoaderDelegate(extDir), extMainClass)));
    }

    if (requestedLibraries.isEmpty()) {
      Path path = Paths.get(FileUtils.LIBRARY_CONFIG_FILE);
      if (Files.isRegularFile(path)) {
        SourceLibrary library = myLibraryResolver.registerLibrary(path.toAbsolutePath().normalize());
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
    boolean doubleCheck = cmdLine.hasOption("c");
    for (SourceLibrary library : requestedLibraries) {
      myModuleResults.clear();
      if (recompile) {
        library.addFlag(SourceLibrary.Flag.RECOMPILE);
      }
      if (!myLibraryManager.loadLibrary(library, typechecking)) {
        continue;
      }

      List<Concrete.Definition> forcedDefs;
      if (recompileModule != null) {
        List<TCReferable> forcedRefs = new ArrayList<>();
        if (recompileDef != null) {
          Scope scope = library.getModuleScopeProvider().forModule(recompileModule);
          if (scope == null && library.loadTests(myLibraryManager, Collections.singletonList(recompileModule))) {
            scope = library.getTestsModuleScopeProvider().forModule(recompileModule);
          }
          if (scope == null) {
            System.err.println("[ERROR] Cannot find module '" + recompileModule + "' in library '" + library.getName() + "'");
          } else {
            Referable ref = Scope.Utils.resolveName(scope, recompileDef.toList());
            if (!(ref instanceof TCReferable)) {
              System.err.println("[ERROR] Cannot find definition '" + recompileDef + "' in module '" + recompileModule + "' in library '" + library.getName() + "'");
            } else {
              forcedRefs.add((TCReferable) ref);
            }
          }
        } else {
          Group group = library.getModuleGroup(recompileModule, false);
          if (group == null && library.loadTests(myLibraryManager, Collections.singletonList(recompileModule))) {
            group = library.getModuleGroup(recompileModule, true);
          }
          if (group == null) {
            System.err.println("[ERROR] Cannot find module '" + recompileModule + "' in library '" + library.getName() + "'");
          } else {
            group.traverseGroup(g -> {
              LocatedReferable ref = g.getReferable();
              if (ref instanceof TCReferable) {
                forcedRefs.add((TCReferable) ref);
              }
            });
          }
        }

        forcedDefs = new ArrayList<>();
        for (TCReferable ref : forcedRefs) {
          Concrete.ReferableDefinition def = typechecking.getConcreteProvider().getConcrete(ref);
          if (def instanceof Concrete.Definition) {
            forcedDefs.add((Concrete.Definition) def);
            Definition typechecked = ref.getTypechecked();
            ref.setTypechecked(null);
            if (typechecked != null) {
              for (Definition recursive : typechecked.getRecursiveDefinitions()) {
                recursive.getRef().setTypechecked(null);
              }
            }
          }
        }
      } else {
        forcedDefs = null;
      }

      Collection<? extends ModulePath> modules = library.getUpdatedModules();
      int numWithErrors = 0;
      if (!modules.isEmpty() || forcedDefs != null) {
        System.out.println();
        System.out.println("--- Typechecking " + library.getName() + " ---");
        long time = System.currentTimeMillis();
        if (forcedDefs == null) {
          typechecking.typecheckLibrary(library);
        } else {
          typechecking.typecheckDefinitions(forcedDefs, null);
        }
        time = System.currentTimeMillis() - time;
        flushErrors();

        // Output nice per-module typechecking results
        int numWithGoals = 0;
        for (ModulePath module : modules) {
          GeneralError.Level result = myModuleResults.get(module);
          if (result == null && library.getModuleGroup(module, false) == null && library.getModuleGroup(module, true) == null) {
            result = GeneralError.Level.ERROR;
          }
          reportTypeCheckResult(module, result);
          if (result == GeneralError.Level.ERROR) numWithErrors++;
          if (result == GeneralError.Level.GOAL) numWithGoals++;
        }

        if (numWithErrors > 0) {
          myExitWithError = true;
          System.out.println("Number of modules with errors: " + numWithErrors);
        }
        if (numWithGoals > 0) {
          System.out.println("Number of modules with goals: " + numWithGoals);
        }
        System.out.println("--- Done (" + timeToString(time) + ") ---");

        // Persist updated modules
        if (library.supportsPersisting()) {
          library.persistUpdatedModules(mySystemErrErrorReporter);
        }
      }

      if (doubleCheck && numWithErrors == 0) {
        System.out.println();
        System.out.println("--- Checking " + library.getName() + " ---");
        long time = System.currentTimeMillis();

        CoreModuleChecker checker = new CoreModuleChecker(myErrorReporter);
        for (ModulePath module : library.getLoadedModules()) {
          Group group = library.getModuleGroup(module, false);
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
      for (SourceLibrary library : requestedLibraries) {
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
        if (doubleCheck) {
          boolean doCheck = true;
          for (GeneralError error : myErrorReporter.getErrorList()) {
            if (error.level == GeneralError.Level.ERROR) {
              doCheck = false;
              break;
            }
          }
          if (doCheck) {
            CoreModuleChecker checker = new CoreModuleChecker(myErrorReporter);
            for (ModulePath module : modules) {
              Group group = library.getModuleGroup(module, true);
              if (group != null) {
                checker.checkGroup(group);
              }
            }
          }
        }

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
      PrettyPrinterConfigWithRenamer ppConfig = new PrettyPrinterConfigWithRenamer(EmptyScope.INSTANCE);
      if (error instanceof GoalError) {
        ppConfig.expressionFlags = EnumSet.of(PrettyPrinterFlag.SHOW_FIELD_INSTANCE);
      }
      if (error.level == GeneralError.Level.ERROR) {
        myExitWithError = true;
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

  private void updateSourceResult(ModuleLocation moduleLocation, GeneralError.Level result) {
    if (moduleLocation == null) {
      return;
    }

    ModulePath module = moduleLocation.getModulePath();
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
