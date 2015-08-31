package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;

import java.io.IOException;

public class GeneralError {
  private final Namespace myNamespace;
  private final String myMessage;

  public GeneralError(Namespace namespace, String message) {
    myNamespace = namespace;
    myMessage = message;
  }

  public String getMessage() {
    return myMessage;
  }

  public Namespace getNamespace() {
    return myNamespace;
  }

  public String printPosition() {
    return myNamespace == null ? "" : myNamespace.getFullName() + ": ";
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }

  @Override
  public String toString() {
    return printPosition() + (myMessage == null ? "Unknown error" : myMessage);
  }
}
