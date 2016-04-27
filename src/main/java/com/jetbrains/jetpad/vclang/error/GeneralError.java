package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public class GeneralError {
  private final String myMessage;
  private Level myLevel;

  public enum Level { ERROR, WARNING, INFO }

  public GeneralError(String message) {
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

  public String printHeader() {
    return "[" + myLevel + "] ";
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }

  @Override
  public String toString() {
    return printHeader() + (myMessage == null ? "Unknown error" : myMessage);
  }
}
