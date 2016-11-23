package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.*;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.module.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.CompositeSourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.CompositeStorage;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.file.FileStorage;
import com.jetbrains.jetpad.vclang.module.utils.LoadModulesRecursively;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.SimpleTypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ConsoleMain {
  private static Options cmdOptions = new Options();
  static {
    cmdOptions.addOption("h", "help", false, "print this message");
    cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("dir").desc("source directory").build());
    //cmdOptions.addOption(Option.builder("o").longOpt("output").hasArg().argName("dir").desc("output directory").build());
    //cmdOptions.addOption(Option.builder("L").hasArg().argName("dir").desc("add <dir> to the list of directories searched for libraries").build());
    cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
  }

  private final List<String> argFiles;

  private final ListErrorReporter errorReporter = new ListErrorReporter();
  private final ErrorFormatter errf;

  // Storage
  private final String sourceDirStr;
  private final File sourceDir;
  private final CompositeStorage<Prelude.SourceId, FileStorage.SourceId> storage;

  // Modules
  private final SourceModuleLoader<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> moduleLoader;
  private final Map<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId, Map<String, Abstract.Definition>> definitionIds = new HashMap<>();

  // Name resolving
  private final SimpleModuleNamespaceProvider moduleNsProvider  = new SimpleModuleNamespaceProvider();
  private final SimpleStaticNamespaceProvider staticNsProvider  = new SimpleStaticNamespaceProvider();
  private final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();

  private final SourceInfoProvider<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> srcInfoProvider;

  // Typechecking
  private final TypecheckerState state = new SimpleTypecheckerState();
  private final Set<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> requestedSources = new LinkedHashSet<>();


  public ConsoleMain(CommandLine cmdLine) {
    OneshotSourceInfoCollector<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> srcInfoCollector = new OneshotSourceInfoCollector<>();
    srcInfoProvider = srcInfoCollector.sourceInfoProvider;
    errf = new ErrorFormatter(srcInfoProvider);

    sourceDirStr = cmdLine.getOptionValue("s");
    sourceDir = new File(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);
    storage = createStorage(sourceDir, errorReporter);

    boolean recompile = cmdLine.hasOption("recompile");
    ModuleLoadingListener moduleLoadingListener = new ModuleLoadingListener(srcInfoCollector);
    moduleLoader = createModuleLoader(storage, moduleLoadingListener);

    Namespace preludeNamespace = loadPrelude();
    moduleLoadingListener.setPreludeNamespace(preludeNamespace);

    argFiles = cmdLine.getArgList();
  }

  private static CompositeStorage<Prelude.SourceId, FileStorage.SourceId> createStorage(File sourceDir, ErrorReporter errorReporter) {
    Prelude.PreludeStorage preludeStorage = new Prelude.PreludeStorage(errorReporter);
    FileStorage fileStorage = new FileStorage(sourceDir, errorReporter);
    return new CompositeStorage<>(preludeStorage, fileStorage, preludeStorage, fileStorage);
  }

  private static SourceModuleLoader<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> createModuleLoader(CompositeStorage<Prelude.SourceId, FileStorage.SourceId> storage, ModuleLoadingListener moduleLoadingListener) {
    return new BaseModuleLoader<>(storage, moduleLoadingListener);
  }

  private Namespace loadPrelude() {
    Abstract.ClassDefinition prelude = moduleLoader.load(Prelude.PreludeStorage.PRELUDE_MODULE_PATH);
    Typechecking.typecheckModules(state, staticNsProvider, dynamicNsProvider, Collections.singletonList(prelude), new DummyErrorReporter(), new TypecheckedReporter.Dummy(), true);
    assert errorReporter.getErrorList().isEmpty();
    return staticNsProvider.forDefinition(prelude);
  }

  private void run() {
    // Collect sources for which typechecking was requested
    final Path sourceDirPath = sourceDir.toPath();
    if (argFiles.isEmpty()) {
      if (sourceDirStr == null) return;
      try {
        Files.walkFileTree(sourceDirPath, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            requestFileTypechecking(sourceDirPath.relativize(path).toFile());
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
      for (String fileName : argFiles) {
        requestFileTypechecking(new File(fileName));
      }
    }

    // Typecheck those sources
    Map<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> typeCheckResults = typeCheckSources(requestedSources);

    flushErrors();

    // Output nice per-module typechecking results
    for (Map.Entry<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> entry : typeCheckResults.entrySet()) {
      if (!requestedSources.contains(entry.getKey())) {
        ModuleResult result = entry.getValue();
        reportTypeCheckResult(entry.getKey(), result == ModuleResult.OK ? ModuleResult.UNKNOWN : result);
      }
    }
    for (CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source : requestedSources) {
      reportTypeCheckResult(source, typeCheckResults.get(source));
    }

    // TODO: Serialize typechecked modules
  }

  private void reportTypeCheckResult(CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source, ModuleResult result) {
    StringBuilder builder = new StringBuilder();
    builder.append("[").append(resultChar(result)).append("]");
    builder.append(" ").append(displaySource(source, true));
    System.out.println(builder);
  }

  private static char resultChar(ModuleResult result) {
    switch (result) {
      case NOT_LOADED:
        return '✗';
      case OK:
        return '✓';
      case GOALS:
        return '◯';
      case ERRORS:
        return '✗';
      default:
        return ' ';
    }
  }

  private Map<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> typeCheckSources(Set<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> sources) {
    final Map<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> results = new LinkedHashMap<>();

    final Set<Abstract.ClassDefinition> modulesToTypeCheck = new LinkedHashSet<>();
    for (CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source : sources) {
      SourceSupplier.Result loadingResult = moduleLoader.load(source);
      flushErrors();
      if (loadingResult == null || loadingResult.definition == null) {
        results.put(source, ModuleResult.NOT_LOADED);
        continue;
      }
      modulesToTypeCheck.add(loadingResult.definition);
    }

    System.out.println("--- Checking ---");

    Typechecking.typecheckModules(state, staticNsProvider, dynamicNsProvider, modulesToTypeCheck, errorReporter, new TypecheckedReporter() {
      @Override
      public void typecheckingSucceeded(Abstract.Definition definition) {
        CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source = srcInfoProvider.sourceOf(definition);
        updateModuleResult(source, ModuleResult.OK);
      }

      @Override
      public void typecheckingFailed(Abstract.Definition definition) {
        CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source = srcInfoProvider.sourceOf(definition);
        boolean goals = false;
        boolean errors = false;
        for (GeneralError error : errorReporter.getErrorList()) {
          if (error.getLevel() == Error.Level.GOAL) {
            goals = true;
          }
          if (error.getLevel() == Error.Level.ERROR) {
            errors = true;
          }
        }
        assert errors || goals;
        results.put(source, errors ? ModuleResult.ERRORS : ModuleResult.GOALS);

        flushErrors();
      }

      private void updateModuleResult(CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source, ModuleResult result) {
        ModuleResult prevResult = results.get(source);
        if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
          results.put(source, result);
        }
      }
    }, false);

    return results;
  }

  private void flushErrors() {
    for (GeneralError error : errorReporter.getErrorList()) {
      System.out.println(errf.printError(error));
    }
    errorReporter.getErrorList().clear();
  }

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  class ModuleLoadingListener extends BaseModuleLoader.ModuleLoadingListener<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> {
    private final OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(errorReporter, new ConcreteResolveListener(), moduleNsProvider, staticNsProvider, dynamicNsProvider);
    private final OneshotSourceInfoCollector<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> mySrcInfoCollector;
    private Namespace preludeNamespace = new EmptyNamespace();

    ModuleLoadingListener(OneshotSourceInfoCollector<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> srcInfoCollector) {
      mySrcInfoCollector = srcInfoCollector;
    }

    void setPreludeNamespace(Namespace preludeNamespace) {
      this.preludeNamespace = preludeNamespace;
    }

    @Override
    public void loadingSucceeded(CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId module, Abstract.ClassDefinition abstractDefinition) {
      if (abstractDefinition != null) {
        if (!definitionIds.containsKey(module)) {
          definitionIds.put(module, new HashMap<String, Abstract.Definition>());
        }

        new LoadModulesRecursively(moduleLoader).visitClass(abstractDefinition, null);

        oneshotNameResolver.visitModule(abstractDefinition, preludeNamespace);
        mySrcInfoCollector.visitModule(module, abstractDefinition);
      }
      moduleNsProvider.registerModule(module.getModulePath(), abstractDefinition);
      System.out.println("[Loaded] " + displaySource(module, false));
    }
  }

  private static String displaySource(CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId source, boolean modulePathOnly) {
    StringBuilder builder = new StringBuilder();
    builder.append(source.getModulePath());
    if (!modulePathOnly && source.source2 != null) {
      builder.append(" (").append(source.source2).append(")");
    }
    return builder.toString();
  }

  private void requestFileTypechecking(File file) {
    if (!file.getName().endsWith(FileStorage.EXTENSION)) return;
    String fileName = file.toString();
    fileName = fileName.substring(0, fileName.length() - FileStorage.EXTENSION.length());

    CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId sourceId = storage.locateModule(FileStorage.modulePath(fileName));
    if (sourceId == null) {
      System.err.println(file + ": incorrect file name");
    } else {
      requestedSources.add(sourceId);
    }
  }


  public static void main(String[] args) {
    try {
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        printHelp();
      } else {
        new ConsoleMain(cmdLine).run();
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
    }
  }

  private static void printHelp() {
    new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
  }

}
