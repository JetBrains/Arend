package com.jetbrains.jetpad.vclang.frontend.storage;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.ModuleRegistry;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileStorage implements Storage<FileStorage.SourceId> {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  public static ModulePath modulePath(Path path) {
    assert !path.isAbsolute();
    List<String> names = new ArrayList<>();
    for (Path elem : path) {
      String name = elem.toString();
      if (!name.matches("[a-zA-Z_][a-zA-Z0-9_']*")) return null;
      names.add(name);
    }

    return new ModulePath(names);
  }

  private static long getLastModifiedTime(Path file) throws IOException {
    return Files.getLastModifiedTime(file).toMillis();
  }

  private static Path baseFile(Path root, ModulePath modulePath) {
    return root.resolve(Paths.get("", modulePath.toArray()));
  }

  public static Path sourceFile(Path base) {
    return base.resolveSibling(base.getFileName() + EXTENSION);
  }

  public static Path cacheFile(Path base) {
    return base.resolveSibling(base.getFileName() + SERIALIZED_EXTENSION);
  }


  private final NameResolver myNameResolver;
  private final ModuleRegistry myModuleRegistry;

  private Scope myGlobalScope = new EmptyScope();
  private final FileSourceSupplier mySourceSupplier;
  private final FileCacheStorageSupplier myCacheStorageSupplier;


  public FileStorage(Path sourceRoot, Path cacheRoot, NameResolver nameResolver, ModuleRegistry moduleRegistry) {
    myNameResolver = nameResolver;
    myModuleRegistry = moduleRegistry;

    mySourceSupplier = new FileSourceSupplier(sourceRoot);
    myCacheStorageSupplier = new FileCacheStorageSupplier(cacheRoot);
  }

  public void setPreludeNamespace(Namespace ns) {
    myGlobalScope = new NamespaceScope(ns);
  }

  private class FileSourceSupplier implements SourceSupplier<SourceId> {
    private final Path myRoot;

    private FileSourceSupplier(Path root) {
      myRoot = root;
    }

    private Path sourceFileForSource(SourceId sourceId) {
      return sourceFile(baseFile(myRoot, sourceId.getModulePath()));
    }

    @Override
    public SourceId locateModule(@Nonnull ModulePath modulePath) {
      Path file = sourceFile(baseFile(myRoot, modulePath));
      if (Files.exists(file)) {
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
        Concrete.ClassDefinition result = fileSource.load(errorReporter, myModuleRegistry, myGlobalScope, myNameResolver);

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
      return cacheFile(baseFile(myRoot, sourceId.getModulePath()));
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
    return mySourceSupplier.locateModule(modulePath);
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
      return sourceFile(baseFile(mySourceSupplier.myRoot, myModulePath)).toString();
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, Path file) throws IOException {
      super(sourceId, Files.newBufferedReader(file, StandardCharsets.UTF_8));
    }
  }
}
