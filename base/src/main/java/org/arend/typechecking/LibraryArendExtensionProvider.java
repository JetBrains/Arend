package org.arend.typechecking;

import org.arend.ext.ArendExtension;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.module.FullModulePath;
import org.arend.naming.reference.TCReferable;
import org.jetbrains.annotations.Nullable;

public class LibraryArendExtensionProvider implements ArendExtensionProvider {
  private final LibraryManager myLibraryManager;
  private final ArendExtension myUniqueArendExtension;

  public LibraryArendExtensionProvider(LibraryManager libraryManager) {
    myLibraryManager = libraryManager;

    Library internalLibrary = null;
    for (Library library : libraryManager.getRegisteredLibraries()) {
      if (!library.isExternal()) {
        if (internalLibrary == null) {
          internalLibrary = library;
        } else {
          myUniqueArendExtension = null;
          return;
        }
      }
    }

    myUniqueArendExtension = internalLibrary == null ? null : internalLibrary.getArendExtension();
  }

  @Override
  public @Nullable ArendExtension getArendExtension(TCReferable ref) {
    if (myUniqueArendExtension != null) {
      return myUniqueArendExtension;
    }

    FullModulePath modulePath = ref.getLocation();
    if (modulePath == null) {
      return null;
    }

    String libraryName = modulePath.getLibraryName();
    if (libraryName == null) {
      return null;
    }

    Library library = myLibraryManager.getRegisteredLibrary(libraryName);
    return library == null || library.isExternal() ? null : library.getArendExtension();
  }
}
