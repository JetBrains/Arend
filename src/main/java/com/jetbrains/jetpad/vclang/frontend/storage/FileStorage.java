package com.jetbrains.jetpad.vclang.frontend.storage;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.ModuleRegistry;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class FileStorage implements Storage<FileStorage.SourceId> {
  private static long getLastModifiedTime(Path file) throws IOException {
    return Files.getLastModifiedTime(file).toMillis();
  }

  private final ModuleRegistry myModuleRegistry;
  private final ModuleScopeProvider myModuleScopeProvider;

  private final FileSourceSupplier mySourceSupplier;
  private final FileCacheStorageSupplier myCacheStorageSupplier;


  public FileStorage(Path sourceRoot, Path cacheRoot, ModuleRegistry moduleRegistry, ModuleScopeProvider moduleScopeProvider) {
    myModuleRegistry = moduleRegistry;
    myModuleScopeProvider = moduleScopeProvider;

    mySourceSupplier = new FileSourceSupplier(sourceRoot);
    myCacheStorageSupplier = new FileCacheStorageSupplier(cacheRoot);
  }

  private class FileSourceSupplier implements SourceSupplier<SourceId> {
    private final Path myRoot;

    private FileSourceSupplier(Path root) {
      myRoot = root;
    }

    private Path sourceFileForSource(SourceId sourceId) {
      return FileUtils.sourceFile(myRoot, sourceId.getModulePath());
    }

    @Override
    public SourceId locateModule(@Nonnull ModulePath modulePath) {
      Path sourceFile = FileUtils.sourceFile(myRoot, modulePath);
      if (Files.exists(sourceFile)) {
        return new SourceId(modulePath);
      }
      return null;
    }

    @Override
    public boolean isAvailable(@Nonnull SourceId sourceId) {
      if (sourceId.getStorage() != FileStorage.this) return false;
      Path file = sourceFileForSource(sourceId);
      return Files.exists(file);
    }

    @Override
    public LoadResult loadSource(@Nonnull SourceId sourceId, @Nonnull ErrorReporter errorReporter) {
      try {
        if (!isAvailable(sourceId)) return null;

        Path file = sourceFileForSource(sourceId);
        long mtime = getLastModifiedTime(file);

        FileSource fileSource = new FileSource(sourceId, file);
        ChildGroup result = fileSource.load(errorReporter, myModuleRegistry, myModuleScopeProvider);

        // Make sure file did not change
        if (getLastModifiedTime(file) != mtime) return null;
        return LoadResult.make(result, mtime);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public long getAvailableVersion(@Nonnull SourceId sourceId) {
      if (!isAvailable(sourceId)) return 0;
      try {
        return getLastModifiedTime(sourceFileForSource(sourceId));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private class FileCacheStorageSupplier implements CacheStorageSupplier<SourceId> {
    private final Path myRoot;

    private FileCacheStorageSupplier(Path root) {
      myRoot = root;
    }

    private Path cacheFileForSource(SourceId sourceId) {
      return FileUtils.cacheFile(myRoot, sourceId.getModulePath());
    }

    @Override
    public InputStream getCacheInputStream(SourceId sourceId) {
      if (myRoot == null) return null;
      if (sourceId.getStorage() != FileStorage.this) return null;
      Path file = cacheFileForSource(sourceId);
      if (Files.isReadable(file)) {
        try {
          return Files.newInputStream(file);
        } catch (IOException ignored) {
        }
      }
      return null;
    }

    @Override
    public OutputStream getCacheOutputStream(SourceId sourceId) {
      if (myRoot == null) return null;
      if (sourceId.getStorage() != FileStorage.this) return null;
      Path file = cacheFileForSource(sourceId);
      try {
        Files.createDirectories(file.getParent());
        return Files.newOutputStream(file);
      } catch (IOException ignored) {
      }
      return null;
    }

    public SourceId locateModule(ModulePath modulePath) {
      Path cacheFile = FileUtils.cacheFile(myRoot, modulePath);
      if (Files.exists(cacheFile)) {
        return new SourceId(modulePath);
      }
      return null;
    }
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    return myCacheStorageSupplier.getCacheInputStream(sourceId);
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    return myCacheStorageSupplier.getCacheOutputStream(sourceId);
  }

  @Override
  public SourceId locateModule(@Nonnull ModulePath modulePath) {
    SourceId sourceId = mySourceSupplier.locateModule(modulePath);
    if (sourceId != null) {
      return sourceId;
    } else {
      return myCacheStorageSupplier.locateModule(modulePath);
    }
  }

  @Override
  public boolean isAvailable(@Nonnull SourceId sourceId) {
    return mySourceSupplier.isAvailable(sourceId);
  }

  @Override
  public LoadResult loadSource(@Nonnull SourceId sourceId, @Nonnull ErrorReporter errorReporter) {
    return mySourceSupplier.loadSource(sourceId, errorReporter);
  }

  @Override
  public long getAvailableVersion(@Nonnull SourceId sourceId) {
    return mySourceSupplier.getAvailableVersion(sourceId);
  }


  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final ModulePath myModulePath;

    private SourceId(ModulePath modulePath) {
      myModulePath = modulePath;
    }

    private FileStorage getStorage() {
      return FileStorage.this;
    }

    @Override
    public ModulePath getModulePath() {
      return myModulePath;
    }

    public Path getRelativeFilePath() {
      return Paths.get("", myModulePath.toArray());
    }

    @Override
    public boolean equals(Object o) {
      return o == this ||
             o instanceof SourceId &&
             getStorage().equals(((SourceId) o).getStorage()) &&
             myModulePath.equals(((SourceId) o).myModulePath);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getStorage(), myModulePath);
    }

    @Override
    public String toString() {
      return FileUtils.sourceFile(mySourceSupplier.myRoot, myModulePath).toString();
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, Path file) throws IOException {
      super(sourceId, Files.newBufferedReader(file, StandardCharsets.UTF_8));
    }
  }
}
