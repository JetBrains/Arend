package org.arend.error;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocStringBuilder;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.DataContainer;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public abstract class Error {
  public final String message;
  public final @Nonnull Level level;

  public enum Level { INFO, WARNING, GOAL, ERROR }

  public Error(@Nonnull Level level, String message) {
    this.level = level;
    this.message = message;
  }

  public Object getCause() {
    return null;
  }

  public LineDoc getPositionDoc(PrettyPrinterConfig ppConfig) {
    Object cause = getCause();
    Object data = cause instanceof SourceInfo ? cause : cause instanceof DataContainer ? ((DataContainer) cause).getData() : null;
    return data instanceof SourceInfo ? refDoc(new SourceInfoReference((SourceInfo) data)) : empty();
  }

  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return text(message);
  }

  public final LineDoc getHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hSep(text(" "), text("[" + level + "]"), hEnd(text(":"), getPositionDoc(ppConfig)), getShortHeaderDoc(ppConfig));
  }

  public Doc getCauseDoc(PrettyPrinterConfig ppConfig) {
    return null;
  }

  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return nullDoc();
  }

  protected Doc getDoc(PrettyPrinterConfig ppConfig, LineDoc header, Doc cause) {
    return vHang(header, vList(getBodyDoc(ppConfig), cause == null ? nullDoc() : hang(text("In:"), cause)));
  }

  public Doc getDoc(PrettyPrinterConfig ppConfig) {
    return getDoc(ppConfig, getHeaderDoc(ppConfig), getCauseDoc(ppConfig));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(PrettyPrinterConfig.DEFAULT));
  }

  public Doc getShortDoc() {
    return getDoc(PrettyPrinterConfig.DEFAULT, text(message), null);
  }

  public String getShortMessage() {
    return DocStringBuilder.build(getShortDoc());
  }

  public boolean isTypecheckingError() {
    return false;
  }
}
