package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

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
    builder.append(printHeader()).append(getMessage()).append(":\n");

    String msg = "Expected type: ";
    int length = msg.length() + PrettyPrintVisitor.INDENT / 2;
    PrettyPrintVisitor.printIndent(builder, PrettyPrintVisitor.INDENT / 2);
    builder.append(msg);
    myExpected.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, length);
    builder.append('\n');
    PrettyPrintVisitor.printIndent(builder, PrettyPrintVisitor.INDENT / 2);
    builder.append("  Actual type: ");
    myActual.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, length);

    String ppClause = prettyPrint(getCause());
    if (ppClause != null) {
      builder.append('\n');
      PrettyPrintVisitor.printIndent(builder, PrettyPrintVisitor.INDENT / 2);
      builder.append("In expression: ").append(ppClause);
    }

    return builder.toString();
  }
}
