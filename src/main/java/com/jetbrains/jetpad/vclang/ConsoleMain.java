package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.*;
import com.jetbrains.jetpad.vclang.module.source.file.FileStorage;
import com.jetbrains.jetpad.vclang.module.utils.LoadModulesRecursively;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.SimpleTypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.order.TypecheckingOrdering;
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

  public static void main(String[] args) {
    final CommandLine cmdLine;
    try {
      cmdLine = new DefaultParser().parse(cmdOptions, args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      return;
    }

    if (cmdLine.hasOption("h")) {
      new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
    }

    final String sourceDirStr = cmdLine.getOptionValue("s");
    final File sourceDir = new File(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);
    boolean recompile = cmdLine.hasOption("recompile");

    final SimpleStaticNamespaceProvider staticNsProvider = new SimpleStaticNamespaceProvider();
    final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
    final ListErrorReporter errorReporter = new ListErrorReporter();
    final SimpleModuleNamespaceProvider moduleNsProvider = new SimpleModuleNamespaceProvider();
    final OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(errorReporter, new ConcreteResolveListener(), moduleNsProvider, staticNsProvider, dynamicNsProvider);
    final OneshotSourceInfoCollector<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> srcInfoCollector = new OneshotSourceInfoCollector<>();
    final ErrorFormatter errf = new ErrorFormatter(srcInfoCollector.sourceInfoProvider);
    final List<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> loadedModules = new ArrayList<>();
    final List<Abstract.Definition> modulesToTypeCheck = new ArrayList<>();
    final Map<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId, Map<String, Abstract.Definition>> definitionIds = new HashMap<>();

    // Storage
    final Prelude.PreludeStorage preludeStorage = new Prelude.PreludeStorage(errorReporter);
    final FileStorage fileStorage = new FileStorage(sourceDir, errorReporter);
    final CompositeStorage<Prelude.SourceId, FileStorage.SourceId> storage = new CompositeStorage<>(preludeStorage, fileStorage, preludeStorage, fileStorage);

    class ModuleLoadingListener extends BaseModuleLoader.ModuleLoadingListener<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> {
      private ModuleLoader moduleLoader = null;
      private Namespace preludeNamespace = new EmptyNamespace();

      private void setModuleLoader(ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
      }
      private void setPreludeNamespace(Namespace preludeNamespace) {
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
          srcInfoCollector.visitModule(module, abstractDefinition);

          modulesToTypeCheck.add(abstractDefinition);
        }
        moduleNsProvider.registerModule(module.getModulePath(), abstractDefinition);
        loadedModules.add(module);
        System.out.println("[Loaded] " + module.getModulePath());
      }
    }
    ModuleLoadingListener moduleLoadingListener = new ModuleLoadingListener();

    final BaseModuleLoader<CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId> moduleLoader = new BaseModuleLoader<>(storage, moduleLoadingListener);
    moduleLoadingListener.setModuleLoader(moduleLoader);

    final Abstract.ClassDefinition prelude = moduleLoader.load(Prelude.PreludeStorage.PRELUDE_MODULE_PATH);
    final Namespace preludeNamespace = staticNsProvider.forDefinition(prelude);
    moduleLoadingListener.setPreludeNamespace(preludeNamespace);

    final TypecheckerState state = new SimpleTypecheckerState();

    TypecheckingOrdering.typecheck(state, staticNsProvider, dynamicNsProvider, Collections.singletonList(prelude), new DummyErrorReporter(), true);
    assert errorReporter.getErrorList().isEmpty();


    if (!errorReporter.getErrorList().isEmpty()) {
      for (GeneralError error : errorReporter.getErrorList()) {
        System.err.println(errf.printError(error));
      }
      return;
    }

    final Path sourceDirPath = sourceDir.toPath();
    if (cmdLine.getArgList().isEmpty()) {
      if (sourceDirStr == null) return;
      try {
        Files.walkFileTree(sourceDirPath, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            if (path.toString().endsWith(FileStorage.EXTENSION)) {
              processFile(storage, moduleLoader, errorReporter, errf, sourceDirPath.relativize(path).toFile());
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
        processFile(storage, moduleLoader, errorReporter, errf, new File(fileName));
      }
    }

    final Map<SourceId, ModuleResult> failedModules = new HashMap<>();

    TypecheckingOrdering.typecheck(state, staticNsProvider, dynamicNsProvider, modulesToTypeCheck, errorReporter, new TypecheckedReporter() {
      @Override
      public void typecheckingSucceeded(Abstract.Definition definition) {

      }

      @Override
      public void typecheckingFailed(Abstract.Definition definition) {
        boolean goals = false;
        boolean errors = false;
        for (GeneralError error : errorReporter.getErrorList()) {
          if (error.getLevel() == Error.Level.GOAL) {
            goals = true;
          }
          if (error.getLevel() == Error.Level.ERROR) {
            errors = true;
          }
          System.err.println(errf.printError(error));
        }

        if (errors || goals) {
          failedModules.put(srcInfoCollector.sourceInfoProvider.sourceOf(definition), errors ? ModuleResult.ERRORS : ModuleResult.GOALS);
        }
        errorReporter.getErrorList().clear();
      }
    }, false);

    for (CompositeSourceSupplier<Prelude.SourceId, FileStorage.SourceId>.SourceId moduleId : loadedModules) {
      StringBuilder builder = new StringBuilder();
      ModuleResult result = failedModules.get(moduleId);
      builder.append("[").append(result == ModuleResult.ERRORS ? "✗" : result == ModuleResult.GOALS ? "o" : " ").append("]");
      builder.append(" ").append(moduleId.getModulePath());
      if (moduleId.source2 != null) {
        builder.append(" (").append(moduleId.source2).append(")");
      }
      System.out.println(builder);
    }

    for (GeneralError error : errorReporter.getErrorList()) {
      System.err.println(errf.printError(error));
    }

    // TODO: Serialize typechecked modules
  }

  private enum ModuleResult { GOALS, ERRORS, OK }

  static private <SourceIdT extends SourceId> void processFile(SourceSupplier<SourceIdT> storage, SourceModuleLoader<SourceIdT> moduleLoader, ListErrorReporter errorReporter, ErrorFormatter errf, File file) {
    if (!file.getName().endsWith(FileStorage.EXTENSION)) return;
    String fileName = file.toString();
    fileName = fileName.substring(0, fileName.length() - FileStorage.EXTENSION.length());

    SourceIdT sourceId = storage.locateModule(FileStorage.modulePath(fileName));
    if (sourceId == null) {
      System.err.println(file + ": incorrect file name");
      return;
    }

    moduleLoader.load(sourceId);

    for (GeneralError error : errorReporter.getErrorList()) {
      System.err.println(errf.printError(error));
    }
    errorReporter.getErrorList().clear();
  }


}
