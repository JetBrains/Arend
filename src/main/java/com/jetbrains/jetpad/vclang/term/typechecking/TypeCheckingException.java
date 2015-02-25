package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class TypeCheckingException extends RuntimeException {
  private final Expression expression;

  public TypeCheckingException(Expression expression) {
    this.expression = expression;
  }

  public Expression getExpression() {
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
