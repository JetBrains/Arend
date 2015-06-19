package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class ParserError extends ModuleError {
  private final Concrete.Position myPosition;

  public ParserError(Module module, Concrete.Position position, String message) {
    super(module, message);
    myPosition = position;
  }

  @Override
  public String toString() {
    String msg = getModule().getFile(null, ".vc") + ":" + myPosition.line + ":" + myPosition.column + ": Parser error";
    if (getMessage() != null) {
      msg += ": " + getMessage();
    }
    return msg;
  }
}
