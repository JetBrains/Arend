package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.nullDoc;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class LibraryIOError extends GeneralError {
  public final String fileName;
  public final String longMessage;

  public LibraryIOError(String fileName, String shortMessage, String longMessage) {
    super(Level.ERROR, shortMessage);
    this.fileName = fileName;
    this.longMessage = longMessage;
  }

  public LibraryIOError(String fileName, String message) {
    this(fileName, message, null);
  }

  @Override
  public LineDoc getPositionDoc(PrettyPrinterConfig ppConfig) {
    return text(fileName);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return longMessage != null ? text(longMessage) : nullDoc();
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.emptyList();
  }
}
