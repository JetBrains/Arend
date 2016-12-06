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

public class FileStorage implements SourceSupplier<FileStorage.SourceId>, CacheStorageSupplier<FileStorage.SourceId> {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  private final Path myRoot;

  public FileStorage(Path root) {
    myRoot = root;
  }

  public static ModulePath modulePath(String pathString) {
    Path path = Paths.get(pathString);
    int nameCount = path.getNameCount();
    if (nameCount < 1) return null;
    List<String> names = new ArrayList<>(nameCount);
    for (int i = 0; i < nameCount; ++i) {
      String name = path.getName(i).toString();
      if (name.length() == 0 || !(Character.isLetterOrDigit(name.charAt(0)) || name.charAt(0) == '_')) return null;
      for (int j = 1; j < name.length(); ++j) {
        if (!(Character.isLetterOrDigit(name.charAt(j)) || name.charAt(j) == '_' || name.charAt(j) == '-' || name.charAt(j) == '\'')) return null;
      }
      names.add(name);
    }
    return new ModulePath(names);
  }

  private Path baseFileForModule(ModulePath modulePath) {
    return myRoot.resolve(Paths.get("", modulePath.list()));
  }

  private Path sourceFileForModule(ModulePath modulePath) {
    Path base = baseFileForModule(modulePath);
    return base.resolveSibling(base.getFileName() + EXTENSION);
  }

  private Path cacheFileForSource(SourceId sourceId) {
    Path base = baseFileForModule(sourceId.getModulePath());
    return base.resolveSibling(base.getFileName() + "." + sourceId.myMtime.toMillis() + SERIALIZED_EXTENSION);
  }

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    Path file = sourceFileForModule(modulePath);
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
    Path file = sourceFileForModule(sourceId.myModulePath);
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

    Path file = sourceFileForModule(sourceId.myModulePath);
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
      return sourceFileForModule(myModulePath).toString();
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, Path file) throws IOException {
      super(sourceId, Files.newBufferedReader(file, StandardCharsets.UTF_8));
    }
  }
}
