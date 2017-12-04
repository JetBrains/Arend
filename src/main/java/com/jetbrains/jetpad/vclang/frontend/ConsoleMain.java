package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.LibStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.ModuleCacheIdProvider;
import com.jetbrains.jetpad.vclang.module.source.CompositeSourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.CompositeStorage;
import com.jetbrains.jetpad.vclang.module.source.NullStorage;
import org.apache.commons.cli.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

public class ConsoleMain extends BaseCliFrontend<CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId> {
  private static final Options cmdOptions = new Options();
  static {
    cmdOptions.addOption("h", "help", false, "print this message");
    cmdOptions.addOption(Option.builder("L").longOpt("libs").hasArg().argName("libdir").desc("directory containing libraries").build());
    cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("srcdir").desc("project source directory").build());
    cmdOptions.addOption(Option.builder("c").longOpt("cache").hasArg().argName("cachedir").desc("directory for project-specific cache files (relative to srcdir)").build());
    cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
  }

  private final StorageManager storageManager;

  public ConsoleMain(Path libDir, Path sourceDir, Path cacheDir) throws IOException {
    this.storageManager = new StorageManager(libDir, sourceDir, cacheDir);
    initialize(storageManager.storage);
  }

  @Override
  protected String displaySource(CompositeSourceSupplier<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId source, boolean modulePathOnly) {
    StringBuilder builder = new StringBuilder();
    builder.append(source.getModulePath());
    if (!modulePathOnly) {
      if (source.source1 != null) {
        builder.append(" (").append(source.source1).append(")");
      } else if (source.source2 != null && source.source2.source1 != null) {
        builder.append(" (").append(source.source2.source1).append(")");
      }
    }
    return builder.toString();
  }

  @Override
  protected ModuleCacheIdProvider<CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId> createModuleUriProvider() {
    return new MyModuleCacheIdProvider();
  }

  private class StorageManager {
    final FileStorage projectStorage;
    final LibStorage libStorage;
    final PreludeStorage preludeStorage;

    private final CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId> nonProjectCompositeStorage;
    public final CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId> storage;

    StorageManager(Path libDir, Path projectDir, Path cacheDir) throws IOException {
      projectStorage = new FileStorage(projectDir, cacheDir, moduleTracker, moduleScopeProvider);
      libStorage = libDir != null ? new LibStorage(libDir, moduleTracker, moduleScopeProvider) : null;
      preludeStorage = new PreludeStorage(moduleTracker);

      nonProjectCompositeStorage = new CompositeStorage<>(libStorage != null ? libStorage : new NullStorage<>(), preludeStorage);
      storage = new CompositeStorage<>(projectStorage, nonProjectCompositeStorage);
    }

    CompositeSourceSupplier<FileStorage.SourceId, CompositeSourceSupplier<com.jetbrains.jetpad.vclang.frontend.storage.LibStorage.SourceId, com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage.SourceId>.SourceId>.SourceId idForProjectSource(FileStorage.SourceId sourceId) {
      return storage.idFromFirst(sourceId);
    }

    CompositeSourceSupplier<FileStorage.SourceId, CompositeSourceSupplier<com.jetbrains.jetpad.vclang.frontend.storage.LibStorage.SourceId, com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage.SourceId>.SourceId>.SourceId idForLibSource(LibStorage.SourceId sourceId) {
      if (libStorage == null) return null;
      return storage.idFromSecond(nonProjectCompositeStorage.idFromFirst(sourceId));
    }

    CompositeSourceSupplier<FileStorage.SourceId, CompositeSourceSupplier<com.jetbrains.jetpad.vclang.frontend.storage.LibStorage.SourceId, com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage.SourceId>.SourceId>.SourceId idForPreludeSource(PreludeStorage.SourceId sourceId) {
      return storage.idFromSecond(nonProjectCompositeStorage.idFromSecond(sourceId));
    }
  }

  private class MyModuleCacheIdProvider implements ModuleCacheIdProvider<CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId> {
    @Override
    public @Nonnull String getCacheId(CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId sourceId) {
      if (sourceId.source1 != null) {  // Project source
        return "file:" + sourceId.source1.getModulePath();
      } else {
        if (sourceId.source2 == null) throw new IllegalStateException();
        if (sourceId.source2.source1 != null) {  // Lib source
          return "lib:" + sourceId.source2.source1.getLibraryName() + " " + sourceId.source2.source1.fileSourceId.getModulePath();
        } else {  // Prelude source
          return "prelude";
        }
      }
    }

    @Override
    public @Nullable CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId getModuleId(String cacheId) {
      if (cacheId.startsWith("file:")) {
        try {
          ModulePath modulePath = new ModulePath(Arrays.asList(cacheId.substring(5).split("\\.")));
          FileStorage.SourceId fileSourceId = storageManager.projectStorage.locateModule(modulePath);
          return fileSourceId != null ? storageManager.idForProjectSource(fileSourceId) : null;
        } catch (NumberFormatException e) {
          return null;
        }
      } else if (cacheId.startsWith("lib:")) {
        if (storageManager.libStorage == null) return null;
        try {
          cacheId = cacheId.substring(4);
          int index = cacheId.indexOf(' ');
          if (index == -1) return null;
          String libName = cacheId.substring(0, index);
          ModulePath modulePath = new ModulePath(Arrays.asList(cacheId.substring(index + 1).split("\\.")));
          LibStorage.SourceId libSourceId = storageManager.libStorage.locateModule(libName, modulePath);
          return libSourceId != null ? storageManager.idForLibSource(libSourceId) : null;
        } catch (NumberFormatException e) {
          return null;
        }
      } else if (cacheId.equals("prelude")) {
        return storageManager.idForPreludeSource(storageManager.preludeStorage.preludeSourceId);
      } else {
        return null;
      }
    }

  }


  public static void main(String[] args) throws IOException {
    try {
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        printHelp();
      } else {
        String libDirStr = cmdLine.getOptionValue("L");
        Path libDir = libDirStr != null ? Paths.get(libDirStr) : null;

        String sourceDirStr = cmdLine.getOptionValue("s");
        Path sourceDir = Paths.get(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);

        String cacheDirStr = cmdLine.getOptionValue("c");
        Path cacheDir = sourceDir.resolve(cacheDirStr != null ? cacheDirStr : ".cache");

        if (cmdLine.hasOption("recompile")) {
          deleteCache(cacheDir);
        }

        new ConsoleMain(libDir, sourceDir, cacheDir).run(sourceDir, cmdLine.getArgList());
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
    }
  }

  private static void deleteCache(Path cacheDir) throws IOException {
    if (!Files.exists(cacheDir)) {
      return;
    }

    Files.walkFileTree(cacheDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.getFileName().toString().endsWith(FileStorage.SERIALIZED_EXTENSION)) {
          try {
            Files.delete(file);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static void printHelp() {
    new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
  }
}
