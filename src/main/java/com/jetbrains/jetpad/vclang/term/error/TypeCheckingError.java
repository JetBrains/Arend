package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class TypeCheckingError {
  private final String myMessage;
  private final Abstract.Expression myExpression;

  public TypeCheckingError(String message, Abstract.Expression expression) {
    myMessage = message;
    myExpression = expression;
  }

  public Abstract.Expression getExpression() {
    return myExpression;
  }

  public String getMessage() {
    return myMessage;
  }

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
