package org.arend.library;

import org.arend.module.ModulePath;
import org.arend.util.Range;

import java.util.Collection;
import java.util.List;

/**
 * Represents a library header.
 *
 */
public class LibraryHeader {
  public final Collection<ModulePath> modules;
  public final List<LibraryDependency> dependencies;
  public final Range<String> languageVersionRange;

  public LibraryHeader(Collection<ModulePath> modules, List<LibraryDependency> dependencies, Range<String> languageVersionRange) {
    this.modules = modules;
    this.dependencies = dependencies;
    this.languageVersionRange = languageVersionRange;
  }
}
