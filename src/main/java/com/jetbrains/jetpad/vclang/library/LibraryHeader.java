package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.util.Collection;
import java.util.List;

/**
 * Represent a library header.
 *
 */
public class LibraryHeader {
  public final Collection<ModulePath> modules;
  public final List<LibraryDependency> dependencies;

  public LibraryHeader(Collection<ModulePath> modules, List<LibraryDependency> dependencies) {
    this.modules = modules;
    this.dependencies = dependencies;
  }
}
