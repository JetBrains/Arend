package com.jetbrains.jetpad.vclang;

import java.io.IOException;

public class VcError {
  private final String myMessage;

  public VcError(String message) {
    myMessage = message;
  }

  public String getMessage() {
    return myMessage;
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }

  @Override
  public String toString() {
    return myMessage == null ? "" : myMessage;
  }
}
