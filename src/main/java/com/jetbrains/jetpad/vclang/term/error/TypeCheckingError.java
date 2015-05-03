package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class TypeCheckingError {
  private final String myMessage;
  private final Abstract.SourceNode myExpression;

  public TypeCheckingError(String message, Abstract.SourceNode expression) {
    myMessage = message;
    myExpression = expression;
  }

  public Abstract.SourceNode getExpression() {
    return myExpression;
  }

  public String getMessage() {
    return myMessage;
  }

  // TODO: Replace myExpression.toString() with pretty printing.
  @Override
  public String toString() {
    String msg = myMessage == null ? "Type checking error" : myMessage;
    if (myExpression == null) {
      return msg;
    } else {
      return msg + " in " + myExpression;
    }
  }
}
