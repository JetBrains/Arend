package org.arend.frontend.library;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.frontend.source.ZipFileRawSource;
import org.arend.library.*;
import org.arend.library.classLoader.ZipClassLoaderDelegate;
import org.arend.library.error.LibraryIOError;
import org.arend.module.error.ExceptionError;
import org.arend.source.*;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.util.FileUtils;
import org.arend.util.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipSourceLibrary extends UnmodifiableSourceLibrary {
  private final File myFile;
  private Version myVersion;
  private String mySourcesDir = "";
  private String myBinariesDir;
  private ZipFile myZipFile;
  private ZipClassLoaderDelegate myClassLoaderDelegate;
  private List<LibraryDependency> myDependencies = Collections.emptyList();
  private Set<ModulePath> myModules = Collections.emptySet();

  public ZipSourceLibrary(String name, File zipFile) {
    super(name);
    myFile = zipFile;
  }

  @Override
  public String getFullName() {
    return myFile.getPath();
  }

  @Override
  public boolean isExternal() {
    return true;
  }

  @Override
  public @Nullable Source getRawSource(ModulePath modulePath) {
    ZipEntry entry = myZipFile.getEntry(mySourcesDir + String.join("/", modulePath.toList()) + FileUtils.EXTENSION);
    return entry == null ? null : new ZipFileRawSource(modulePath, myZipFile, entry);
  }

  @Override
  public @Nullable BinarySource getBinarySource(ModulePath modulePath) {
    if (myBinariesDir == null) {
      return null;
    }
    ZipEntry entry = myZipFile.getEntry(myBinariesDir + String.join("/", modulePath.toList()) + FileUtils.SERIALIZED_EXTENSION);
    return entry == null ? null : new GZIPStreamBinarySource(new ZipFileBinarySource(modulePath, myZipFile, entry));
  }

  @Override
  public @Nullable PersistableBinarySource getPersistableBinarySource(ModulePath modulePath) {
    return null;
  }

  @Override
  public @Nullable Version getVersion() {
    return myVersion;
  }

  @Override
  public boolean load(LibraryManager libraryManager, TypecheckingOrderingListener typechecking) {
    if (isLoaded()) {
      return true;
    }

    try (ZipFile zipFile = new ZipFile(myFile)) {
      myZipFile = zipFile;
      return super.load(libraryManager, typechecking);
    } catch (IOException e) {
      libraryManager.getLibraryErrorReporter().report(new ExceptionError(e, "loading of library " + getName()));
      return false;
    } finally {
      myZipFile = null;
      if (myClassLoaderDelegate != null) {
        myClassLoaderDelegate.zipFile = null;
      }
    }
  }

  @Override
  public @NotNull Collection<? extends LibraryDependency> getDependencies() {
    return myDependencies;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return myModules.contains(modulePath);
  }

  @Override
  protected @Nullable LibraryHeader loadHeader(ErrorReporter errorReporter) {
    if (myZipFile == null) {
      return null;
    }

    ZipEntry yamlEntry = myZipFile.getEntry(FileUtils.LIBRARY_CONFIG_FILE);
    if (yamlEntry == null) {
      errorReporter.report(new LibraryIOError(myFile.getPath(), "Cannot find arend.yaml in zip file"));
      return null;
    }

    LibraryConfig config;
    try (InputStream stream = myZipFile.getInputStream(yamlEntry)) {
      config = new YAMLMapper().readValue(stream, LibraryConfig.class);
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, "loading of library " + getName()));
      return null;
    }

    LibraryHeader header = LibraryHeader.fromConfig(config, myFile.getPath(), errorReporter);
    if (header == null) {
      return null;
    }

    String sourcesDir = config.getSourcesDir();
    if (sourcesDir != null) {
      sourcesDir = sourcesDir.replace('\\', '/');
      mySourcesDir = sourcesDir.isEmpty() || sourcesDir.endsWith("/") ? sourcesDir : sourcesDir + "/";
    }

    myVersion = Version.fromString(config.getVersion());

    myBinariesDir = config.getBinariesDir();
    if (myBinariesDir != null && !myBinariesDir.isEmpty()) {
      myBinariesDir = myBinariesDir.replace('\\', '/');
      if (!myBinariesDir.endsWith("/")) {
        myBinariesDir = myBinariesDir + "/";
      }
    }

    if (header.modules == null) {
      header.modules = new LinkedHashSet<>();
      Enumeration<? extends ZipEntry> entries = myZipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String name = entry.getName();
        if (name.length() > mySourcesDir.length() + FileUtils.EXTENSION.length() && name.startsWith(mySourcesDir) && name.endsWith(FileUtils.EXTENSION)) {
          header.modules.add(FileUtils.modulePath(name.substring(mySourcesDir.length(), name.length() - FileUtils.EXTENSION.length()).replace('/', '.')));
        }
      }
    }

    if (config.getExtensionsDir() != null) {
      myClassLoaderDelegate = new ZipClassLoaderDelegate(myFile, myZipFile, config.getExtensionsDir());
      header.classLoaderDelegate = myClassLoaderDelegate;
    }

    myDependencies = header.dependencies;
    myModules = new LinkedHashSet<>(header.modules);
    return header;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZipSourceLibrary that = (ZipSourceLibrary) o;
    return myFile.equals(that.myFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myFile);
  }
}
