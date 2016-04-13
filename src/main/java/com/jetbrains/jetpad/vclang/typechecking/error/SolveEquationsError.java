package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;

public class SolveEquationsError extends TypeCheckingError {
  private final Expression myExpr1;
  private final Expression myExpr2;
  private final Binding myBinding;

  public SolveEquationsError(Expression expr1, Expression expr2, Binding binding, Abstract.SourceNode expression) {
    super(null, expression);
    myExpr1 = expr1;
    myExpr2 = expr2;
    myBinding = binding;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader());
    String msg = "\t1st expression: ";
    builder.append("Cannot solve equation:\n").append(msg);
    myExpr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, msg.length());
    builder.append('\n')
        .append("\t2nd expression: ");
    myExpr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, msg.length());
    if (myBinding != null) {
      builder.append('\n')
          .append("\tSince '").append(myBinding).append("' is free in these expressions");
    }

    String ppClause = prettyPrint(getCause());
    if (ppClause != null) {
      builder.append('\n')
          .append("\tIn expression: ").append(ppClause);
    }
    return builder.toString();
  }
}
