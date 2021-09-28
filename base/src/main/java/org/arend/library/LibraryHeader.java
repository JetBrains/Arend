package org.arend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.library.classLoader.ClassLoaderDelegate;
import org.arend.library.error.LibraryIOError;
import org.arend.util.FileUtils;
import org.arend.util.Range;
import org.arend.util.Version;
import org.arend.util.VersionRange;

import java.util.*;

/**
 * Represents a library header.
 *
 */
public class LibraryHeader {
  public Collection<ModulePath> modules;
  public final List<LibraryDependency> dependencies;
  public final Version version;
  public final Range<Version> languageVersionRange;
  public ClassLoaderDelegate classLoaderDelegate;
  public final String extMainClass;

  public LibraryHeader(Collection<ModulePath> modules, List<LibraryDependency> dependencies, Version version, Range<Version> languageVersionRange, ClassLoaderDelegate classLoaderDelegate, String extMainClass) {
    this.modules = modules;
    this.dependencies = dependencies;
    this.version = version;
    this.languageVersionRange = languageVersionRange;
    this.classLoaderDelegate = classLoaderDelegate;
    this.extMainClass = extMainClass;
  }

  public static LibraryHeader fromConfig(LibraryConfig config, String fileName, ErrorReporter errorReporter) {
    Collection<ModulePath> modules = null;
    if (config.getModules() != null) {
      modules = new LinkedHashSet<>();
      for (String module : config.getModules()) {
        ModulePath modulePath = FileUtils.modulePath(module);
        if (modulePath != null) {
          modules.add(modulePath);
        } else {
          errorReporter.report(new LibraryIOError(fileName, "Illegal module name: " + module));
        }
      }
    }

    List<LibraryDependency> dependencies = new ArrayList<>();
    if (config.getDependencies() != null) {
      for (String library : config.getDependencies()) {
        if (FileUtils.isLibraryName(library)) {
          dependencies.add(new LibraryDependency(library));
        } else {
          errorReporter.report(new LibraryIOError(fileName, "Illegal library name: " + library));
        }
      }
    }

    Version version = null;
    if (config.getVersion() != null) {
      version = Version.fromString(config.getVersion());
      if (version == null) {
        errorReporter.report(new LibraryIOError(fileName, "Cannot parse version: " + config.getVersion()));
        return null;
      }
    }

    Range<Version> languageVersion = Range.unbound();
    if (config.getLangVersion() != null) {
      languageVersion = VersionRange.parseVersionRange(config.getLangVersion());
      if (languageVersion == null) {
        errorReporter.report(new LibraryIOError(fileName, "Cannot parse language version: " + config.getLangVersion()));
        return null;
      }
    }

    return new LibraryHeader(modules, dependencies, version, languageVersion, null, config.getExtensionMainClass());
  }
}
