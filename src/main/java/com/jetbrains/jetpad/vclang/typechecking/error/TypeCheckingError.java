package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;

public class TypeCheckingError extends GeneralError {
  private Abstract.Definition myDefinition;
  private final Abstract.SourceNode myExpression;

  public TypeCheckingError(Abstract.Definition definition, String message, Abstract.SourceNode expression) {
    super(message);
    myDefinition = definition;
    myExpression = expression;
  }

  public TypeCheckingError(String message, Abstract.SourceNode expression) {
    this(null, message, expression);
  }

  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  public void setDefinition(Abstract.Definition definition) {
    myDefinition = definition;
  }

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
    return new PrettyPrintVisitor(builder, 0).prettyPrint(node, Abstract.Expression.PREC) ? builder.toString() : null;
  }

  @Override
  public String printHeader() {
    StringBuilder msg = new StringBuilder(super.printHeader());
    if (myDefinition != null) {
      msg.append(myDefinition).append(":");
    }
    if (myExpression instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) myExpression).getPosition();
      if (position != null) {
        msg.append(position.line).append(":").append(position.column).append(":");
      }
    }
    msg.append(" ");
    return msg.toString();
  }

  @Override
  public String toString() {
    String msg = super.toString();
    String ppExpr = prettyPrint(myExpression);
    return ppExpr != null ? msg + " in " + ppExpr : msg;
  }
}
