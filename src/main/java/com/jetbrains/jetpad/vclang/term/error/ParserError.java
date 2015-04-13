package com.jetbrains.jetpad.vclang.term.error;

public class ParserError {
  private final int myLine;
  private final int myPos;
  private final String myMessage;

  public ParserError(int line, int pos, String message) {
    myLine = line;
    myPos = pos;
    myMessage = message;
  }

  @Override
  public String toString() {
    String msg = myLine + ":" + myPos + ": Parser error";
    if (myMessage != null) {
      msg += ": " + myMessage;
    }
    return msg;
  }
}
