package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;

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

  @Override
  public final String toString() {
    return "ERROR: " + getMessage();
  }
}
