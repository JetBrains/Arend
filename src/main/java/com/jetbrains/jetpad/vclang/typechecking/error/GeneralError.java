package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.IOException;

public class GeneralError {
  private ResolvedName myResolvedName;
  private final String myMessage;
  private Level myLevel;

  public enum Level { ERROR, WARNING, INFO }

  public GeneralError(ResolvedName resolvedName, String message) {
    myResolvedName = resolvedName;
    myMessage = message;
    myLevel = Level.ERROR;
  }

  public GeneralError(String message) {
    myResolvedName = null;
    myMessage = message;
    myLevel = Level.ERROR;
  }

  public Level getLevel() {
    return myLevel;
  }

  public void setLevel(Level level) {
    myLevel = level;
  }

  public Abstract.SourceNode getCause() {
    return null;
  }

  public String getMessage() {
    return myMessage;
  }

  public ResolvedName getResolvedName() {
    return myResolvedName;
  }

  public void setResolvedName(ResolvedName resolvedName) {
    myResolvedName = resolvedName;
  }

  public String printHeader() {
    return "[" + myLevel + "] " + (myResolvedName == null || myResolvedName.parent == null ? "" : myResolvedName + ": ");
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }

  @Override
  public String toString() {
    return printHeader() + (myMessage == null ? "Unknown error" : myMessage);
  }
}
