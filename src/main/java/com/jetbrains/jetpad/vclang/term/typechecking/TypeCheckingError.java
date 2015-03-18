package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class TypeCheckingError {
  private final Abstract.Expression expression;
  private final String message;

  public TypeCheckingError(String message, Abstract.Expression expression) {
    this.message = message;
    this.expression = expression;
  }

  public Abstract.Expression getExpression() {
    return expression;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    String msg = message == null ? "Type checking error" : message;
    if (expression == null) {
      return msg;
    } else {
      return msg + " in " + expression;
    }
  }
}
