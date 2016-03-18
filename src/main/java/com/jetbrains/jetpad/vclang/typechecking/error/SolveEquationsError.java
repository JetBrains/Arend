package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;

public class SolveEquationsError extends TypeCheckingError {
  private final Expression myExpr1;
  private final Expression myExpr2;
  private final Binding myBinding;
  private final boolean myTypeOf;

  public SolveEquationsError(Expression expr1, Expression expr2, Binding binding, Abstract.SourceNode expression, boolean typeOf) {
    super(null, expression);
    myExpr1 = expr1;
    myExpr2 = expr2;
    myBinding = binding;
    myTypeOf = typeOf;
  }

  public SolveEquationsError(Expression expr1, Expression expr2, Binding binding, Abstract.SourceNode expression) {
    this(expr1, expr2, binding, expression, false);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader());
    builder.append("Cannot solve equation:\n")
        .append("\t1st expression: ");
    myExpr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    builder.append('\n')
        .append("\t2nd expression: ");
    if (myTypeOf) {
      builder.append("type of ");
    }
    myExpr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
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
