package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;

import java.util.ArrayList;

public class TypeMismatchError extends TypeCheckingError {
  private final PrettyPrintable myExpected;
  private final PrettyPrintable myActual;

  public TypeMismatchError(PrettyPrintable expected, PrettyPrintable actual, Abstract.Expression expression) {
    super("Type mismatch", expression);
    myExpected = expected;
    myActual = actual;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader()).append(getMessage());
    builder.append(":\n")
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
