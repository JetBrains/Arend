package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.List;

public class TypeMismatchError extends TypeCheckingError {
  private final Object myExpected;
  private final Abstract.Expression myActual;

  public TypeMismatchError(Definition parent, Object expected, Abstract.Expression actual, Abstract.Expression expression, List<String> names) {
    super(parent, null, expression, names);
    myExpected = expected;
    myActual = actual;
  }

  @Override
  public String toString() {
    String message = printPosition();
    message += "Type mismatch:\n" +
        "\tExpected type: " + (myExpected instanceof Abstract.Expression ? prettyPrint((Abstract.Expression) myExpected) : myExpected.toString()) + "\n" +
        "\t  Actual type: " + prettyPrint(myActual);
    if (getExpression() instanceof Abstract.PrettyPrintableSourceNode) {
      message += "\n" +
          "\tIn expression: " + prettyPrint((Abstract.PrettyPrintableSourceNode) getExpression());
    }
    return message;
  }
}
