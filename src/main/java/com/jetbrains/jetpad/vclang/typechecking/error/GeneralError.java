package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public class GeneralError {
  private Namespace myNamespace;
  private final String myMessage;
  private Level myLevel;

  public enum Level { ERROR, WARNING, INFO }

  public GeneralError(Namespace namespace, String message) {
    myNamespace = namespace;
    myMessage = message;
    myLevel = Level.ERROR;
  }

  public GeneralError(String message) {
    this(null, message);
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

  public Namespace getNamespace() {
    return myNamespace;
  }

  public void setNamespace(Namespace namespace) {
    myNamespace = namespace;
  }

  public String printHeader() {
    return "[" + myLevel + "] " + (myNamespace == null ? "" : myNamespace.getFullName() + ": ");
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }

  @Override
  public String toString() {
    return printHeader() + (myMessage == null ? "Unknown error" : myMessage);
  }
}
