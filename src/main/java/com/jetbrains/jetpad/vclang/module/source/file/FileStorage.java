package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileStorage implements SourceSupplier<FileStorage.SourceId>, CacheStorageSupplier<FileStorage.SourceId> {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  private final File myRoot;

  public FileStorage(File root) {
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

  private File baseFileFromPath(ModulePath modulePath) {
    File file = myRoot;
    for (String s : modulePath.list()) {
      file = new File(file, s);
    }
    return file;
  }

  private File sourceFileFromPath(ModulePath modulePath) {
    return new File(baseFileFromPath(modulePath).toPath() + EXTENSION);
  }

  private File cacheFileFromPath(ModulePath modulePath, long mtime) {
    return new File(baseFileFromPath(modulePath).toPath() + SERIALIZED_EXTENSION + "-" + mtime);
  }

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    File file = sourceFileFromPath(modulePath);
    if (file.exists()) {
      return new SourceId(modulePath, file.lastModified());
    } else {
      return null;
    }
  }

  public SourceId locateModule(ModulePath modulePath, long mtime) {
    return new SourceId(modulePath, mtime);
  }

  @Override
  public boolean isAvailable(SourceId sourceId) {
    if (sourceId.getStorage() != this) return false;
    File file = sourceFileFromPath(sourceId.myModulePath);
    return file.exists() && file.lastModified() == sourceId.myMtime;
  }

  @Override
  public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
    if (sourceId.getStorage() != this) return null;
    File file = sourceFileFromPath(sourceId.myModulePath);
    if (file.exists()) {
      try {
        FileSource fileSource = new FileSource(sourceId, file);
        Abstract.ClassDefinition definition = fileSource.load(errorReporter);
        // Make sure we loaded the right revision
        return file.lastModified() == sourceId.myMtime ? definition : null;
      } catch (FileNotFoundException ignored) {
      }
    }
    return null;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId.getStorage() != this) return null;
    File file = cacheFileFromPath(sourceId.myModulePath, sourceId.myMtime);
    if (file.exists()) {
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException ignored) {
      }
    }
    return null;
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    if (sourceId.getStorage() != this) return null;
    File file = cacheFileFromPath(sourceId.myModulePath, sourceId.myMtime);
    try {
      //noinspection ResultOfMethodCallIgnored
      file.createNewFile();
      if (file.canWrite()) {
          return new FileOutputStream(file);
      }
    } catch (IOException ignored) {
    }
    return null;
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

    public Path getFilePath() {
      return myRoot.toPath().relativize(baseFileFromPath(myModulePath).toPath());
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
      return sourceFileFromPath(myModulePath).toString();
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, File file) throws FileNotFoundException {
      super(sourceId, new FileInputStream(file));
    }
  }
}
