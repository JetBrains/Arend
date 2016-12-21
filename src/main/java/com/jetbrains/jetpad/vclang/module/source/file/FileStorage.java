package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FileStorage implements SourceSupplier<FileStorage.SourceId>, CacheStorageSupplier<FileStorage.SourceId> {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  private final Path myRoot;

  public FileStorage(Path root) {
    myRoot = root;
  }

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

  public static Path sourceFile(Path base) {
    return base.resolveSibling(base.getFileName() + EXTENSION);
  }

  public static Path cacheFile(Path base, long mtime) {
    return base.resolveSibling(base.getFileName() + "." + mtime + SERIALIZED_EXTENSION);
  }

  private Path sourceFileForSource(SourceId sourceId) {
    return sourceFile(baseFile(sourceId.getModulePath()));
  }

  private Path cacheFileForSource(SourceId sourceId) {
    return cacheFile(baseFile(sourceId.getModulePath()), sourceId.myMtime.toMillis());
  }

  private Path baseFile(ModulePath modulePath) {
    return myRoot.resolve(Paths.get("", modulePath.list()));
  }

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    Path file = sourceFile(baseFile(modulePath));
    try {
      if (Files.exists(file)) {
          return new SourceId(modulePath, Files.getLastModifiedTime(file));
      }
    } catch (IOException ignored) {
    }
    return null;
  }

  public SourceId locateModule(ModulePath modulePath, long mtime) {
    return new SourceId(modulePath, FileTime.fromMillis(mtime));
  }

  @Override
  public boolean isAvailable(SourceId sourceId) {
    if (sourceId.getStorage() != this) return false;
    Path file = sourceFileForSource(sourceId);
    try {
      return Files.exists(file) && Files.getLastModifiedTime(file).equals(sourceId.myMtime);
    } catch (IOException ignored) {
    }
    return false;
  }

  @Override
  public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
    if (sourceId.getStorage() != this) return null;
    if (!isAvailable(sourceId)) return null;

    Path file = sourceFileForSource(sourceId);
    FileSource fileSource = new FileSource(sourceId, file);
    Abstract.ClassDefinition definition = fileSource.load(errorReporter);
    // Make sure we loaded the right revision
    return Files.getLastModifiedTime(file).equals(sourceId.myMtime) ? definition : null;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId.getStorage() != this) return null;
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
    if (sourceId.getStorage() != this) return null;
    Path file = cacheFileForSource(sourceId);
    try {
      Files.createDirectories(file.getParent());
      return Files.newOutputStream(file);
    } catch (IOException ignored) {
    }
    return null;
  }


  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final ModulePath myModulePath;
    private final FileTime myMtime;

    private SourceId(ModulePath modulePath, FileTime mtime) {
      myModulePath = modulePath;
      myMtime = FileTime.from(mtime.toMillis(), TimeUnit.MILLISECONDS);
    }

    private FileStorage getStorage() {
      return FileStorage.this;
    }

    @Override
    public ModulePath getModulePath() {
      return myModulePath;
    }

    public Path getRelativeFilePath() {
      return Paths.get("", myModulePath.list());
    }

    public long getLastModified() {
      return myMtime.toMillis();
    }

    @Override
    public boolean equals(Object o) {
      return o == this ||
             o instanceof SourceId &&
             getStorage().equals(((SourceId) o).getStorage()) &&
             myModulePath.equals(((SourceId) o).myModulePath) &&
             myMtime.equals(((SourceId) o).myMtime);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getStorage(), myModulePath, myMtime);
    }

    @Override
    public String toString() {
      return sourceFile(baseFile(myModulePath)).toString();
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, Path file) throws IOException {
      super(sourceId, Files.newBufferedReader(file, StandardCharsets.UTF_8));
    }
  }
}
