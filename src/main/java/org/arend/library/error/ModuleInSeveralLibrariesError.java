package org.arend.library.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.LineDoc;
import org.arend.library.Library;
import org.arend.module.ModulePath;
import org.arend.naming.reference.ModuleReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.List;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class ModuleInSeveralLibrariesError extends GeneralError {
  public ModulePath modulePath;
  public List<Library> libraries;

  public ModuleInSeveralLibrariesError(ModulePath modulePath, List<Library> libraries) {
    super(Level.WARNING, "Module '" + modulePath + "' is contained in several libraries: ");
    this.modulePath = modulePath;
    this.libraries = libraries;
  }

  @Override
  public ModuleReferable getCause() {
    return new ModuleReferable(modulePath);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    List<LineDoc> libraryDocs = libraries.stream().map(lib -> text(lib.getName())).collect(Collectors.toList());
    return libraryDocs.isEmpty() ? text(message) : hList(text(message), text(": "), hSep(text(", "), libraryDocs));
  }
}
