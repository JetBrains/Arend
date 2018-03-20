package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LibraryReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class LibraryError extends GeneralError {
  public final Stream<String> libraryNames;

  private LibraryError(String message, Stream<String> libraryNames) {
    super(Level.ERROR, message);
    this.libraryNames = libraryNames;
  }

  public static LibraryError cyclic(Stream<String> libraryNames) {
    return new LibraryError("Cyclic dependencies in libraries", libraryNames);
  }

  public static LibraryError notFound(String libraryName) {
    return new LibraryError("Library not found", Stream.of(libraryName));
  }

  public static LibraryError unloadDuringLoading(Stream<String> libraryNames) {
    return new LibraryError("Cannot unload a library while loading other libraries", libraryNames);
  }

  public static LibraryError illegalName(String libraryName) {
    return new LibraryError("Illegal library name or path", Stream.of(libraryName));
  }

  public static LibraryError moduleNotFound(ModulePath modulePath, String libraryName) {
    return new LibraryError("Module '" + modulePath + "' is not found in library", Stream.of(libraryName));
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    List<LineDoc> libraryDocs = libraryNames.map(DocFactory::text).collect(Collectors.toList());
    return libraryDocs.isEmpty()
      ? super.getHeaderDoc(src)
      : hList(super.getHeaderDoc(src), text(": "), hSep(text(", "), libraryDocs));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return libraryNames.map(LibraryReferable::new).collect(Collectors.toList());
  }
}
