package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

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
    return cause instanceof SourceInfo ? refDoc(new SourceInfoReference((SourceInfo) cause)) : empty();
  }

  public LineDoc getHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hSep(text(" "), text("[" + level + "]"), hEnd(text(":"), getPositionDoc(ppConfig)), text(message));
  }

  public Doc getCauseDoc(PrettyPrinterConfig infoProvider) {
    return null;
  }

  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return nullDoc();
  }

  public Doc getDoc(PrettyPrinterConfig ppConfig) {
    Doc cause = getCauseDoc(ppConfig);
    return vHang(getHeaderDoc(ppConfig), vList(getBodyDoc(ppConfig), cause == null ? nullDoc() : hang(text("In:"), cause)));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(PrettyPrinterConfig.DEFAULT));
  }
}
