package org.arend.library;

import org.arend.ext.module.ModulePath;
import org.arend.util.Range;
import org.arend.util.Version;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Represents a library header.
 *
 */
public class LibraryHeader {
  public final Collection<ModulePath> modules;
  public final List<LibraryDependency> dependencies;
  public final Range<Version> languageVersionRange;
  public final Path extBasePath;
  public final String extMainClass;

  public LibraryHeader(Collection<ModulePath> modules, List<LibraryDependency> dependencies, Range<Version> languageVersionRange, Path extBasePath, String extMainClass) {
    this.modules = modules;
    this.dependencies = dependencies;
    this.languageVersionRange = languageVersionRange;
    this.extBasePath = extBasePath;
    this.extMainClass = extMainClass;
  }
}
