package org.arend.library.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

import static org.arend.error.doc.DocFactory.nullDoc;
import static org.arend.error.doc.DocFactory.text;

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
