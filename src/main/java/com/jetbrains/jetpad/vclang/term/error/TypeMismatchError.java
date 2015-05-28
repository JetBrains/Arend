package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class TypeMismatchError extends TypeCheckingError {
  private final Object myExpected;
  private final Abstract.Expression myActual;

  public TypeMismatchError(Object expected, Abstract.Expression actual, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    myExpected = expected;
    myActual = actual;
  }

  @Override
  public String toString() {
    String message = "Type mismatch:\n" +
        "\tExpected type: " + (myExpected instanceof Abstract.Expression ? prettyPrint((Abstract.Expression) myExpected) : myExpected.toString()) + "\n" +
        "\tActual type: " + prettyPrint(myActual);
    if (getExpression() instanceof Abstract.PrettyPrintableSourceNode) {
      message += "\n" +
          "\tIn expression: " + prettyPrint((Abstract.PrettyPrintableSourceNode) getExpression());
    }
    return message;
  }
}
