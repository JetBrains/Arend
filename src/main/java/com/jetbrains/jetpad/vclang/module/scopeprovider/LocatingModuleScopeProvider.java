package com.jetbrains.jetpad.vclang.module.scopeprovider;

import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LocatingModuleScopeProvider implements ModuleScopeProvider {
  private final LibraryManager myLibraryManager;

  public LocatingModuleScopeProvider(LibraryManager libraryManager) {
    myLibraryManager = libraryManager;
  }

  @Nullable
  @Override
  public Scope forModule(@Nonnull ModulePath module) {
    for (Library library : myLibraryManager.getRegisteredLibraries()) {
      if (library.containsModule(module)) {
        return library.getModuleScopeProvider().forModule(module);
      }
    }
    return null;
  }
}
