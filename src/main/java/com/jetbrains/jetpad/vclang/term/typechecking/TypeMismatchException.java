package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class TypeMismatchException extends TypeCheckingException {
  private final Abstract.Expression expected;
  private final Abstract.Expression actual;

  public TypeMismatchException(Abstract.Expression expected, Abstract.Expression actual, Abstract.Expression expression) {
    super(null, expression);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public String toString() {
    String message = "Type mismatch:\n" +
        "Expected type: " + expected + "\n" +
        "Actual type: " + actual;
    if (getExpression() != null) {
      message += "\n" +
          "In expression: " + getExpression();
    }
    return message;
  }
}
