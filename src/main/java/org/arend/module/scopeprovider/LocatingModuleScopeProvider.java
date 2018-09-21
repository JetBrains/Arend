package org.arend.module.scopeprovider;

import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.module.ModulePath;
import org.arend.naming.scope.Scope;

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
    Library library = myLibraryManager.getModuleLibrary(module);
    return library != null ? library.getModuleScopeProvider().forModule(module) : null;
  }
}
