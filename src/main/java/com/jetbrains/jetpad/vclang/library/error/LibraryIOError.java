package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class LibraryIOError extends GeneralError {
  public final String fileName;

  public LibraryIOError(String fileName, String message) {
    super(Level.ERROR, message);
    this.fileName = fileName;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return text(message + ": " + fileName);
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.emptyList();
  }
}
