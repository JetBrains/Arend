package com.jetbrains.jetpad.vclang.frontend.storage;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.term.Abstract;

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

  public static Path cacheFile(Path base, long mtime) {
    return base.resolveSibling(base.getFileName() + "." + mtime + SERIALIZED_EXTENSION);
  }


  private final FileSourceSupplier mySourceSupplier;
  private final FileCacheStorageSupplier myCacheStorageSupplier;


  public FileStorage(Path root) {
    this(root, root);
  }

  public FileStorage(Path sourceRoot, Path cacheRoot) {
    mySourceSupplier = new FileSourceSupplier(sourceRoot);
    myCacheStorageSupplier = new FileCacheStorageSupplier(cacheRoot);
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
    public SourceId locateModule(ModulePath modulePath) {
      Path file = sourceFile(baseFile(myRoot, modulePath));
      try {
        if (Files.exists(file)) {
          return new SourceId(modulePath, getLastModifiedTime(file));
        }
      } catch (IOException ignored) {
      }
      return null;
    }

    @Override
    public boolean isAvailable(SourceId sourceId) {
      if (sourceId.getStorage() != FileStorage.this) return false;
      Path file = sourceFileForSource(sourceId);
      try {
        return Files.exists(file) && getLastModifiedTime(file) == sourceId.myMtime;
      } catch (IOException ignored) {
      }
      return false;
    }

    @Override
    public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
      if (sourceId.getStorage() != FileStorage.this) return null;
      if (!isAvailable(sourceId)) return null;

      Path file = sourceFileForSource(sourceId);
      FileSource fileSource = new FileSource(sourceId, file);
      Abstract.ClassDefinition definition = fileSource.load(errorReporter);
      // Make sure we loaded the right revision
      return getLastModifiedTime(file) == sourceId.myMtime ? definition : null;
    }
  }

  private class FileCacheStorageSupplier implements CacheStorageSupplier<SourceId> {
    private final Path myRoot;

    private FileCacheStorageSupplier(Path root) {
      myRoot = root;
    }

    private Path cacheFileForSource(SourceId sourceId) {
      return cacheFile(baseFile(myRoot, sourceId.getModulePath()), sourceId.myMtime);
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
  public SourceId locateModule(ModulePath modulePath) {
    return mySourceSupplier.locateModule(modulePath);
  }

  public SourceId locateModule(ModulePath modulePath, long mtime) {
    return new SourceId(modulePath, mtime);
  }

  @Override
  public boolean isAvailable(SourceId sourceId) {
    return mySourceSupplier.isAvailable(sourceId);
  }

  @Override
  public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
    return mySourceSupplier.loadSource(sourceId, errorReporter);
  }


  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final ModulePath myModulePath;
    private final long myMtime;

    private SourceId(ModulePath modulePath, long mtime) {
      myModulePath = modulePath;
      myMtime = mtime;
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

    public long getLastModified() {
      return myMtime;
    }

    @Override
    public boolean equals(Object o) {
      return o == this ||
             o instanceof SourceId &&
             getStorage().equals(((SourceId) o).getStorage()) &&
             myModulePath.equals(((SourceId) o).myModulePath) &&
             myMtime == ((SourceId) o).myMtime;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getStorage(), myModulePath, myMtime);
    }

    @Override
    public String toString() {
      return sourceFile(baseFile(mySourceSupplier.myRoot, myModulePath)) + "@" + myMtime;
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, Path file) throws IOException {
      super(sourceId, Files.newBufferedReader(file, StandardCharsets.UTF_8));
    }
  }
}
