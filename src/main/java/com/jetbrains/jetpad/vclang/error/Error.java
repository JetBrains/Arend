package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import javax.annotation.Nonnull;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public abstract class Error {
  public final Abstract.SourceNode cause;
  public final String message;
  public final Level level;

  public enum Level { ERROR, GOAL, WARNING, INFO }

  public Error(String message, Abstract.SourceNode cause) {
    this(Level.ERROR, message, cause);
  }

  public Error(@Nonnull Level level, String message, Abstract.SourceNode cause) {
    this.level = level;
    this.message = message;
    this.cause = cause;
  }

  public final Level getLevel() {
    return level;
  }

  public final Abstract.SourceNode getCause() {
    return cause;
  }

  public final String getMessage() {
    return message;
  }

  public LineDoc getPositionDoc(SourceInfoProvider src) {
    return cause == null ? empty() : hEnd(text(":"), text(src.moduleOf(cause)), text(src.positionOf(cause)));
  }

  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hSep(text(" "), text("[" + level + "]"), getPositionDoc(src), text(message));
  }

  public Doc getCauseDoc() {
    return cause == null ? DocFactory.nullDoc() : hang(text("In:"), sourceNodeDoc(cause));
  }

  public Doc getBodyDoc(SourceInfoProvider src) {
    return DocFactory.nullDoc();
  }

  public Doc getDoc(SourceInfoProvider src) {
    return DocFactory.vHang(getHeaderDoc(src), DocFactory.vList(getBodyDoc(src), getCauseDoc()));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(SourceInfoProvider.Trivial.TRIVIAL));
  }
}
