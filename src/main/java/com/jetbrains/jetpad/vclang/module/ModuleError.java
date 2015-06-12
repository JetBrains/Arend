package com.jetbrains.jetpad.vclang.module;

public class ModuleError {
  private final Module myModule;
  private final String myMessage;

  public ModuleError(Module module, String message) {
    myModule = module;
    myMessage = message;
  }

  @Override
  public String toString() {
    return myModule + ": " + myMessage;
  }
}
