package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.library.resolver.SimpleLibraryResolver;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.prelude.PreludeResourceLibrary;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.SimpleTypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class BaseCliFrontend {
  protected final ListErrorReporter errorReporter = new ListErrorReporter();

  // Libraries
  private final SimpleLibraryResolver myLibraryResolver = new SimpleLibraryResolver();
  private final ModuleScopeProvider myModuleScopeProvider = null; // TODO
  private final LibraryManager myLibraryManager = new LibraryManager(myLibraryResolver, myModuleScopeProvider, System.err::println);

  // Typechecking
  private final TypecheckerState myTypecheckerState = new SimpleTypecheckerState();
  private final Map<ModulePath, ModuleResult> myModuleResults = new LinkedHashMap<>();

  protected abstract String displaySource(ModulePath module, boolean modulePathOnly);

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  private void typeCheckModules(Collection<ModulePath> modules) {
    final Set<Group> modulesToTypeCheck = new LinkedHashSet<>();
    for (ModulePath module : modules) {
      final Group group;
      SourceSupplier.LoadResult result = loadedSources.get(module);
      if (result == null){
        group = moduleTracker.load(module);
        if (group == null) {
          continue;
        }

        try {
          cacheManager.loadCache(module);
        } catch (CacheLoadingException e) {
          //e.printStackTrace();
        }

        flushErrors();
      } else {
        group = result.group;
      }
      modulesToTypeCheck.add(group);
    }

    System.out.println("--- Checking ---");

    new MyTypechecking(myTypecheckerState).typecheckModules(modulesToTypeCheck);
  }

  private class MyTypechecking extends Typechecking {
    MyTypechecking(TypecheckerState state) {
      super(state, ConcreteReferableProvider.INSTANCE, errorReporter);
    }

    @Override
    public void typecheckingFinished(GlobalReferable referable, Definition definition) {
      flushErrors();
    }
  }


  private void reportTypeCheckResult(SourceIdT source, ModuleResult result) {
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
        return ' ';
      case GOALS:
        return '◯';
      case ERRORS:
        return '✗';
      default:
        return '·';
    }
  }

  public void run(Path sourceDir, Collection<String> argFiles) {
    if (!myLibraryManager.loadLibrary(new PreludeResourceLibrary(myTypecheckerState))) {
      return;
    }

    /*
    List<ModulePath> modules = new ArrayList<>();
    if (mySourceBasePath != null || myBinaryBasePath != null) {
      try {
        Path path = mySourceBasePath != null ? mySourceBasePath : myBinaryBasePath;
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().endsWith(mySourceBasePath != null ? FileUtils.EXTENSION : FileUtils.SERIALIZED_EXTENSION)) {
              ModulePath modulePath = FileUtils.modulePath(path.relativize(file), FileUtils.SERIALIZED_EXTENSION);
              if (modulePath != null) {
                modules.add(modulePath);
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        errorReporter.report(new ExceptionError(e, getName()));
        return null;
      }
    }
    */

    // Collect sources for which typechecking was requested
    LinkedHashSet<ModulePath> requestedModules = new ArrayList<>();
    if (argFiles.isEmpty()) {
      try {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
            if (path.getFileName().toString().endsWith(FileUtils.EXTENSION)) {
              path = sourceDir.relativize(path);
              ModulePath modulePath = FileUtils.modulePath(path, FileUtils.EXTENSION);
              if (modulePath == null) {
                printModuleNotFoundError(path);
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
    } else {
      String separator = FileSystems.getDefault().getSeparator();
      for (String fileName : argFiles) {
        ModulePath modulePath;
        if (fileName.endsWith(FileUtils.EXTENSION) || fileName.contains(separator)) {
          modulePath = FileUtils.modulePath(Paths.get(fileName), FileUtils.EXTENSION);
        } else {
          modulePath = FileUtils.modulePath(fileName);
        }
        if (modulePath == null) {
          printModuleNotFoundError(fileName);
        } else {
          requestedModules.add(modulePath);
        }
      }
    }

    // Typecheck those sources
    typeCheckModules(requestedModules);
    flushErrors();

    // Output nice per-module typechecking results
    int numWithErrors = 0;
    for (Map.Entry<SourceIdT, ModuleResult> entry : myModuleResults.entrySet()) {
      if (!requestedModules.contains(entry.getKey())) {
        ModuleResult result = entry.getValue();
        reportTypeCheckResult(entry.getKey(), result == ModuleResult.OK ? ModuleResult.UNKNOWN : result);
        if (result == ModuleResult.ERRORS) numWithErrors += 1;
      }
    }
    // Explicitly requested sources go last
    for (SourceIdT source : requestedModules) {
      ModuleResult result = myModuleResults.get(source);
      reportTypeCheckResult(source, result == null ? ModuleResult.OK : result);
      if (result == ModuleResult.ERRORS) numWithErrors += 1;
    }
    System.out.println("--- Done ---");
    if (numWithErrors > 0) {
      System.out.println("Number of modules with errors: " + numWithErrors);
    }

    // Persist cache
    for (SourceIdT module : cacheManager.getCachedModules()) {
      try {
        cacheManager.persistCache(module);
      } catch (CachePersistenceException e) {
        e.printStackTrace();
      }
    }
  }

  private static void printModuleNotFoundError(Object module) {
    System.err.println("[Not found] " + module + " is an illegal module path");
  }

  private void flushErrors() {
    for (GeneralError error : errorReporter.getErrorList()) {
      ModuleResult moduleResult = error.level == Error.Level.ERROR ? ModuleResult.ERRORS : error.level == Error.Level.GOAL ? ModuleResult.GOALS : null;
      if (moduleResult != null) {
        for (GlobalReferable referable : error.getAffectedDefinitions()) {
          if (referable instanceof SourceIdReference) {
            //noinspection unchecked
            updateSourceResult((SourceIdT) ((SourceIdReference) referable).sourceId, moduleResult);
          } else {
            updateSourceResult(srcInfoProvider.sourceOf(referable), moduleResult);
          }
        }
      }

      System.out.println(error);
    }
    errorReporter.getErrorList().clear();
  }

  private void updateSourceResult(SourceIdT source, ModuleResult result) {
    ModuleResult prevResult = myModuleResults.get(source);
    if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
      myModuleResults.put(source, result);
    }
  }
}
