package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public class GeneralError {
  public final Abstract.SourceNode cause;
  public final String message;
  public final Level level;

  public enum Level { ERROR, GOAL, WARNING, INFO }

  public GeneralError(String message, Abstract.SourceNode cause) {
    this(Level.ERROR, message, cause);
  }

  public GeneralError(Level level, String message, Abstract.SourceNode cause) {
    this.level = level;
    this.message = message;
    this.cause = cause;
  }

  public Level getLevel() {
    return level;
  }

  public Abstract.SourceNode getCause() {
    return cause;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "ERROR: " + getMessage();
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }
}
