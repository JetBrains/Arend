package org.arend.library.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.library.Library;
import org.arend.naming.reference.ModuleReferable;

import java.util.List;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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
