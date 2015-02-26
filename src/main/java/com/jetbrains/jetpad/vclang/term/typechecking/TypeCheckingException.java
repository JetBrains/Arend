package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class TypeCheckingException extends RuntimeException {
  private final Abstract.Expression expression;

  public TypeCheckingException(Abstract.Expression expression) {
    this.expression = expression;
  }

  public Abstract.Expression getExpression() {
    return expression;
  }

  @Override
  public String toString() {
    String message = "Type checking error";
    if (getExpression() == null) {
      return message;
    } else {
      return message + " in " + getExpression();
    }
  }
}
