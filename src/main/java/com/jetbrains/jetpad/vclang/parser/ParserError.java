package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class ParserError extends GeneralError {
  private final Concrete.Position myPosition;

  public ParserError(Namespace module, Concrete.Position position, String message) {
    super(module, message);
    myPosition = position;
  }

  @Override
  public String toString() {
    String msg = getNamespace() + ":" + myPosition.line + ":" + myPosition.column + ": Parser error";
    if (getMessage() != null) {
      msg += ": " + getMessage();
    }
    return msg;
  }
}
