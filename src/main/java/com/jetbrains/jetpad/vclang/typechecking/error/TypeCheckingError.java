package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;

public class TypeCheckingError extends GeneralError {
  private final Abstract.SourceNode myExpression;

  public TypeCheckingError(ResolvedName resolvedName, String message, Abstract.SourceNode expression) {
    super(resolvedName, message);
    myExpression = expression;
  }

  public TypeCheckingError(String message, Abstract.SourceNode expression) {
    super(message);
    myExpression = expression;
  }

  @Override
  public Abstract.SourceNode getCause() {
    return myExpression;
  }

  protected String prettyPrint(PrettyPrintable expression) {
    StringBuilder builder = new StringBuilder();
    expression.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  protected String prettyPrint(Abstract.SourceNode node) {
    StringBuilder builder = new StringBuilder();
    return new PrettyPrintVisitor(builder, new ArrayList<String>(), 0).prettyPrint(node, Abstract.Expression.PREC) ? builder.toString() : null;
  }

  @Override
  public String printHeader() {
    String msg = super.printHeader();
    if (myExpression instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) myExpression).getPosition();
      if (position != null) {
        msg += position.line + ":" + position.column + ": ";
      }
    }
    return msg;
  }

  @Override
  public String toString() {
    String msg = super.toString();
    String ppExpr = prettyPrint(myExpression);
    return ppExpr != null ? msg + " in " + ppExpr : msg;
  }
}
