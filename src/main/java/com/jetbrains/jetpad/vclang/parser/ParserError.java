package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class ParserError extends VcError {
  private final Concrete.Position myPosition;

  public ParserError(Concrete.Position position, String message) {
    super(message);
    myPosition = position;
  }

  @Override
  public String toString() {
    String msg = myPosition.line + ":" + myPosition.column + ": Parser error";
    if (getMessage() != null) {
      msg += ": " + getMessage();
    }
    return msg;
  }
}
