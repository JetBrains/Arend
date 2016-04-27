package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class ParserError extends ModuleLoadingError {
  private final Concrete.Position myPosition;

  public ParserError(ModuleID module, Concrete.Position position, String message) {
    super(module, message);
    myPosition = position;
  }

  public ParserError(Concrete.Position position, String message) {
    this(null, position, message);
  }

  @Override
  public String toString() {
    String msg = printHeader() + myPosition.line + ":" + myPosition.column + ": Parser error";
    if (getMessage() != null) {
      msg += ": " + getMessage();
    }
    return msg;
  }
}
