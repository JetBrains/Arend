package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ModuleError extends GeneralError {
  public ModulePath modulePath;
  public List<Library> libraries;

  public ModuleError(ModulePath modulePath, List<Library> libraries) {
    super(Level.WARNING, "Module '" + modulePath + "' is contained in several libraries: ");
    this.modulePath = modulePath;
    this.libraries = libraries;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    List<LineDoc> libraryDocs = libraries.stream().map(lib -> text(lib.getName())).collect(Collectors.toList());
    return libraryDocs.isEmpty()
      ? super.getHeaderDoc(src)
      : hList(super.getHeaderDoc(src), text(": "), hSep(text(", "), libraryDocs));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(new ModuleReferable(modulePath));
  }
}
