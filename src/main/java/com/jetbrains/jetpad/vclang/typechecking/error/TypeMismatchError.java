package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

public class TypeMismatchError extends TypeCheckingError {
  private final Object myExpected;
  private final Expression myActual;

  public TypeMismatchError(ResolvedName resolvedName, Object expected, Expression actual, Abstract.Expression expression, List<String> names) {
    super(resolvedName, null, expression, names);
    myExpected = expected;
    myActual = actual;
  }

  public TypeMismatchError(Object expected, Expression actual, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    myExpected = expected;
    myActual = actual;
  }

  @Override
  public String toString() {
    String message = printHeader();
    String ppExpected = myExpected instanceof Abstract.SourceNode ? prettyPrint((Abstract.SourceNode) myExpected) : null;
    if (ppExpected == null) {
      ppExpected = myExpected.toString();
    }
    message += "Type mismatch:\n" +
        "\tExpected type: " + ppExpected + "\n" +
        "\t  Actual type: " + prettyPrint(myActual);

    String ppClause = prettyPrint(getCause());
    if (ppClause != null) {
      message += "\n" +
          "\tIn expression: " + ppClause;
    }
    return message;
  }
}
