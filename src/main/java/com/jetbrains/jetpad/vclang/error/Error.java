package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public abstract class Error {
  public final String message;
  public final Level level;

  public enum Level { INFO, WARNING, GOAL, ERROR }

  public Error(@Nonnull Level level, String message) {
    this.level = level;
    this.message = message;
  }

  public Object getCause() {
    return null;
  }

  public final Level getLevel() {
    return level;
  }

  public final String getMessage() {
    return message;
  }

  public LineDoc getPositionDoc(PrettyPrinterConfig src) {
    Object cause = getCause();
    return cause instanceof SourceInfo ? refDoc(new SourceInfoReference((SourceInfo) cause)) : empty();
  }

  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return hSep(text(" "), text("[" + level + "]"), hEnd(text(":"), getPositionDoc(src)), text(message));
  }

  public Doc getCauseDoc(PrettyPrinterConfig infoProvider) {
    return null;
  }

  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return nullDoc();
  }

  public Doc getDoc(PrettyPrinterConfig src) {
    Doc cause = getCauseDoc(src);
    return vHang(getHeaderDoc(src), vList(getBodyDoc(src), cause == null ? nullDoc() : hang(text("In:"), cause)));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(PrettyPrinterConfig.DEFAULT));
  }
}
