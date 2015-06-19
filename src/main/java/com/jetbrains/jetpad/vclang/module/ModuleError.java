package com.jetbrains.jetpad.vclang.module;

import java.io.IOException;

public class ModuleError {
  private final Module myModule;
  private final String myMessage;

  public ModuleError(Module module, String message) {
    myModule = module;
    myMessage = message;
  }

  public Module getModule() {
    return myModule;
  }

  public String getMessage() {
    return myMessage;
  }

  public static String ioError(IOException e) {
    return "I/O error: " + e.getMessage();
  }

  @Override
  public String toString() {
    return myModule + ": " + (myMessage == null ? "Unknown error" : myMessage);
  }
}
