package com.jetbrains.jetpad.vclang.frontend.storage;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModuleRegistry;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.ModuleResolver;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import net.harawata.appdirs.AppDirsFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LibStorage implements Storage<LibStorage.SourceId> {
  private final Map<String, FileStorage> myLibStorages;

  private static Path findCacheDir() throws IOException {
    return Files.createDirectories(Paths.get(AppDirsFactory.getInstance().getUserCacheDir("vclang", null, "JetBrains")));
  }

  public LibStorage(Path libdir, Collection<String> libs, ModuleRegistry moduleRegistry, ModuleResolver moduleResolver) throws IOException {
    if (!Files.isDirectory(libdir)) {
      throw new IllegalArgumentException("libdir must be an existing directory");
    }
    final Path cacheDir = findCacheDir();
    if (!Files.isWritable(cacheDir)) {
      throw new IllegalStateException("Cache directory is not writable");
    }

    myLibStorages = new HashMap<>(libs.size());
    for (String lib : libs) {
      Path sourcePath = libdir.resolve(lib);
      Path cachePath = cacheDir.resolve(lib);
      myLibStorages.put(lib, new FileStorage(sourcePath, cachePath, moduleRegistry, moduleResolver));
    }
  }

  public LibStorage(Path libdir, ModuleRegistry moduleRegistry, ModuleResolver moduleResolver) throws IOException {
    this(libdir, getAllLibs(libdir), moduleRegistry, moduleResolver);
  }

  private static Collection<String> getAllLibs(Path libdir) throws IOException {
    List<String> files = new ArrayList<>();
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(libdir)) {
      for (Path dir : dirStream) {
        if (Files.isDirectory(dir)) {
          files.add(libdir.relativize(dir).toString());
        }
      }

      return files;
    }
  }

  @Override
  public SourceId locateModule(@Nonnull ModulePath modulePath) {
    LibStorage.SourceId result = null;
    for (Map.Entry<String, FileStorage> entry : myLibStorages.entrySet()) {
      FileStorage.SourceId sourceId = entry.getValue().locateModule(modulePath);
      if (sourceId == null) continue;
      if (result == null) {
        result = new SourceId(entry.getKey(), entry.getValue(), sourceId);
      } else {
        throw new ModuleInMultipleLibraries(modulePath);
      }
    }
    return result;
  }

  public SourceId locateModule(String libName, ModulePath modulePath) {
    FileStorage fileStorage = myLibStorages.get(libName);
    if (fileStorage == null) return null;
    return new SourceId(libName, fileStorage, fileStorage.locateModule(modulePath));
  }

  @Override
  public boolean isAvailable(@Nonnull SourceId sourceId) {
    return sourceId.getLibStorage() == this && sourceId.myFileStorage.isAvailable(sourceId.fileSourceId);
  }

  @Override
  public LoadResult loadSource(@Nonnull SourceId sourceId, @Nonnull ErrorReporter errorReporter) {
    if (sourceId.getLibStorage() != this) return null;
    return sourceId.myFileStorage.loadSource(sourceId.fileSourceId, errorReporter);
  }

  @Override
  public long getAvailableVersion(@Nonnull SourceId sourceId) {
    if (sourceId.getLibStorage() != this) return 0;
    return sourceId.myFileStorage.getAvailableVersion(sourceId.fileSourceId);
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId.getLibStorage() != this) return null;
    return sourceId.myFileStorage.getCacheInputStream(sourceId.fileSourceId);
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    if (sourceId.getLibStorage() != this) return null;
    return sourceId.myFileStorage.getCacheOutputStream(sourceId.fileSourceId);
  }


  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final String myLibraryName;
    private final FileStorage myFileStorage;
    public final FileStorage.SourceId fileSourceId;

    public SourceId(String libraryName, FileStorage storage, FileStorage.SourceId sourceId) {
      myLibraryName = libraryName;
      myFileStorage = storage;
      fileSourceId = sourceId;
    }

    private LibStorage getLibStorage() {
      return LibStorage.this;
    }

    @Override
    public ModulePath getModulePath() {
      return fileSourceId.getModulePath();
    }

    public String getLibraryName() {
      return myLibraryName;
    }

    @Override
    public boolean equals(Object o) {
      return o == this ||
             o instanceof SourceId &&
             getLibStorage().equals(((SourceId) o).getLibStorage()) &&
             fileSourceId.equals(((SourceId) o).fileSourceId);
    }

    @Override
    public int hashCode() {
      return fileSourceId.hashCode();
    }

    @Override
    public String toString() {
      return fileSourceId.toString();
    }
  }

  public static class ModuleInMultipleLibraries extends RuntimeException {
    public ModulePath modulePath;

    ModuleInMultipleLibraries(ModulePath modulePath) {
      super("Multiple libraries provide module " + modulePath);
      this.modulePath = modulePath;
    }
  }
}
