package com.jetbrains.jetpad.vclang.library.resolver;

import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nullable;

public class SearchingModuleLocator implements ModuleLocator {
  private final LibraryManager myLibraryManager;

  public SearchingModuleLocator(LibraryManager libraryManager) {
    myLibraryManager = libraryManager;
  }

  @Nullable
  @Override
  public Library locate(ModulePath modulePath) {
    for (Library library : myLibraryManager.getRegisteredLibraries()) {
      if (library.containsModule(modulePath)) {
        return library;
      }
    }
    return null;
  }
}
