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

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    return new SourceId(modulePath);
  }

  public SourceId locatePath(String pathString) {
    return locateModule(modulePath(pathString));
  }

  @Override
  public Result loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
    if (sourceId.getStorage() != this) return null;
    File file = sourceId.getSourceFile();
    if (file.exists()) {
      try {
        FileSource fileSource = new FileSource(sourceId, file);
        Abstract.ClassDefinition definition = fileSource.load(errorReporter);
        return new Result(getCurrentEtag(sourceId), definition);
      } catch (FileNotFoundException ignored) {
      }
    }
    return null;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId.getStorage() != this) return null;
    File file = sourceId.getCacheFile();
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
    File file = sourceId.getCacheFile();
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

  @Override
  public byte[] getCurrentEtag(SourceId sourceId) {
    if (sourceId.getStorage() != this) return null;
    int long_bytes = Long.SIZE / Byte.SIZE;
    byte[] etag = new byte[long_bytes];
    long mtime = sourceId.getSourceFile().lastModified();
    for (int i = 0; i < long_bytes; ++i) {
      etag[i] = (byte) (mtime >> i);
    }
    return etag;
  }


  // TODO: make private or package-local or I dunno
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

    @Override
    public boolean equals(Object o) {
      return o == this || o instanceof SourceId && getStorage().equals(((SourceId) o).getStorage()) && ((SourceId) o).myModulePath.equals(myModulePath);
    }

    @Override
    public int hashCode() {
      return myModulePath.hashCode();
    }

    @Override
    public String toString() {
      return getFile(EXTENSION).toString();
    }

    private File fileFromPath() {
      File file = myRoot;
      for (String s : myModulePath.list()) {
        file = new File(file, s);
      }
      return file;
    }

    File getFile(String ext) {
      return new File(fileFromPath().toPath() + ext);
    }

    File getSourceFile() {
      return getFile(EXTENSION);
    }

    File getCacheFile() {
      return getFile(SERIALIZED_EXTENSION);
    }

    public Path getPath() {
      return myRoot.toPath().relativize(fileFromPath().toPath());
    }
  }

  private static class FileSource extends ParseSource {
    FileSource(com.jetbrains.jetpad.vclang.module.source.SourceId sourceId, File file) throws FileNotFoundException {
      super(sourceId, new FileInputStream(file));
    }
  }
}
