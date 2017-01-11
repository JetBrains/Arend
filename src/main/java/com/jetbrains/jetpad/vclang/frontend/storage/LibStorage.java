package com.jetbrains.jetpad.vclang.frontend.storage;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.term.Abstract;
import net.harawata.appdirs.AppDirsFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LibStorage implements Storage<LibStorage.SourceId> {
  private final List<FileStorage> myLibStorages;

  private static Path findCacheDir() throws IOException {
    return Files.createDirectories(Paths.get(AppDirsFactory.getInstance().getUserCacheDir("vclang", null, "JetBrains")));
  }

  public LibStorage(Path libdir, Collection<String> libs) throws IOException {
    if (!Files.isDirectory(libdir)) {
      throw new IllegalArgumentException("libdir must be an existing directory");
    }
    final Path cacheDir = findCacheDir();
    if (!Files.isWritable(cacheDir)) {
      throw new IllegalStateException("Cache directory is not writable");
    }

    myLibStorages = new ArrayList<>(libs.size());
    for (String lib : libs) {
      Path sourcePath = libdir.resolve(lib);
      Path cachePath = cacheDir.resolve(lib);
      myLibStorages.add(new FileStorage(sourcePath, cachePath));
    }
  }

  public LibStorage(Path libdir) throws IOException {
    this(libdir, getAllLibs(libdir));
  }

  private static Collection<String> getAllLibs(Path libdir) throws IOException {
    List<String> files = new ArrayList<>();
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(libdir)) {
      for (Path dir : dirStream) {
        files.add(libdir.relativize(dir).toString());
      }

      return files;
    }
  }

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    LibStorage.SourceId result = null;
    for (FileStorage storage : myLibStorages) {
      FileStorage.SourceId sourceId = storage.locateModule(modulePath);
      if (sourceId == null) continue;
      if (result == null) {
        result = new SourceId(storage, sourceId);
      } else {
        throw new IllegalArgumentException("Multiple libraries provide module " + modulePath);  // FIXME[error]
      }
    }
    return result;
  }

  @Override
  public boolean isAvailable(SourceId sourceId) {
    if (sourceId.getLibStorage() != this) return false;
    return sourceId.myStorage.isAvailable(sourceId.mySourceId);
  }

  @Override
  public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
    if (sourceId.getLibStorage() != this) return null;
    return sourceId.myStorage.loadSource(sourceId.mySourceId, errorReporter);
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId.getLibStorage() != this) return null;
    return sourceId.myStorage.getCacheInputStream(sourceId.mySourceId);
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    if (sourceId.getLibStorage() != this) return null;
    return sourceId.myStorage.getCacheOutputStream(sourceId.mySourceId);
  }


  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final FileStorage myStorage;
    private final FileStorage.SourceId mySourceId;

    public SourceId(FileStorage storage, FileStorage.SourceId sourceId) {
      myStorage = storage;
      mySourceId = sourceId;
    }

    private LibStorage getLibStorage() {
      return LibStorage.this;
    }

    @Override
    public ModulePath getModulePath() {
      return mySourceId.getModulePath();
    }

    @Override
    public boolean equals(Object o) {
      return o == this ||
             o instanceof SourceId &&
             getLibStorage().equals(((SourceId) o).getLibStorage()) &&
             mySourceId.equals(((SourceId) o).mySourceId);
    }

    @Override
    public int hashCode() {
      return mySourceId.hashCode();
    }

    @Override
    public String toString() {
      return mySourceId.toString();
    }
  }
}
