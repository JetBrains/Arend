package org.arend.library.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;

import static org.arend.ext.prettyprinting.doc.DocFactory.nullDoc;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

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
}
