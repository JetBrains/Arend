package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class ParserError {
  private final Concrete.Position myPosition;
  private final String myMessage;

  public ParserError(Concrete.Position position, String message) {
    myPosition = position;
    myMessage = message;
  }

  @Override
  public String toString() {
    String msg = myPosition.line + ":" + myPosition.column + ": Parser error";
    if (myMessage != null) {
      msg += ": " + myMessage;
    }
    return msg;
  }
}
