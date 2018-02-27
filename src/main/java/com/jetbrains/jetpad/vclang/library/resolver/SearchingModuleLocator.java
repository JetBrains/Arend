package com.jetbrains.jetpad.vclang.library.resolver;

import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.library.error.ModuleError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SearchingModuleLocator implements ModuleLocator {
  private final LibraryManager myLibraryManager;

  public SearchingModuleLocator(LibraryManager libraryManager) {
    myLibraryManager = libraryManager;
  }

  @Nullable
  @Override
  public Library locate(ModulePath modulePath) {
    Library found = null;
    List<Library> foundLibs = null;
    for (Library library : myLibraryManager.getRegisteredLibraries()) {
      if (!library.containsModule(modulePath)) {
        continue;
      }

      if (found == null) {
        found = library;
      } else {
        if (foundLibs == null) {
          foundLibs = new ArrayList<>();
          foundLibs.add(found);
        }
        foundLibs.add(library);
      }
    }

    if (found == null) {
      myLibraryManager.getErrorReporter().report(new ModuleNotFoundError(modulePath));
    } else if (foundLibs != null) {
      myLibraryManager.getErrorReporter().report(new ModuleError(modulePath, foundLibs));
    }

    return found;
  }
}
