package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;

public class TypeMismatchError extends TypeCheckingError {
  private final Expression myExpected;
  private final Expression myActual;

  public TypeMismatchError(ResolvedName resolvedName, Expression expected, Expression actual, Abstract.Expression expression) {
    super(resolvedName, null, expression);
    myExpected = expected;
    myActual = actual;
  }

  public TypeMismatchError(Expression expected, Expression actual, Abstract.Expression expression) {
    super(null, expression);
    myExpected = expected;
    myActual = actual;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader());
    builder.append("Type mismatch:\n")
        .append("\tExpected type: ");
    myExpected.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    builder.append('\n')
        .append("\t  Actual type: ");
    myActual.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);

    String ppClause = prettyPrint(getCause());
    if (ppClause != null) {
      builder.append('\n')
        .append("\tIn expression: ").append(ppClause);
    }
    return builder.toString();
  }
}
