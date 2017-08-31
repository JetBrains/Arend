package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import javax.annotation.Nonnull;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public abstract class Error<T> {
  public final String message;
  public final Level level;

  public enum Level { ERROR, GOAL, WARNING, INFO }

  public Error(@Nonnull Level level, String message) {
    this.level = level;
    this.message = message;
  }

  public T getCause() {
    return null;
  }

  public final Level getLevel() {
    return level;
  }

  public final String getMessage() {
    return message;
  }

  public LineDoc getPositionDoc(PrettyPrinterInfoProvider src) {
    T cause = getCause();
    return cause instanceof SourceInfo ? hSep(text(":"), text(((SourceInfo) cause).moduleTextRepresentation()), text(((SourceInfo) cause).positionTextRepresentation())) : empty();
  }

  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hSep(text(" "), text("[" + level + "]"), hEnd(text(":"), getPositionDoc(src)), text(message));
  }

  public Doc getCauseDoc(PrettyPrinterInfoProvider infoProvider) {
    return null;
  }

  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return nullDoc();
  }

  public Doc getDoc(PrettyPrinterInfoProvider src) {
    Doc cause = getCauseDoc(src);
    return vHang(getHeaderDoc(src), vList(getBodyDoc(src), cause == null ? nullDoc() : hang(text("In:"), cause)));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(PrettyPrinterInfoProvider.TRIVIAL));
  }
}
