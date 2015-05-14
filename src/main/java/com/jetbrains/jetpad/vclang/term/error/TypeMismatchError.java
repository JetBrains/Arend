package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class TypeMismatchError extends TypeCheckingError {
  private final Object expected;
  private final Abstract.Expression actual;

  public TypeMismatchError(Object expected, Abstract.Expression actual, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public String toString() {
    String message = "Type mismatch:\n" +
        "Expected type: " + (expected instanceof Abstract.Expression ? prettyPrint((Abstract.Expression) expected) : expected.toString()) + "\n" +
        "Actual type: " + prettyPrint(actual);
    if (getExpression() != null) {
      message += "\n" +
          "In expression: " + prettyPrint(getExpression());
    }
    return message;
  }
}
