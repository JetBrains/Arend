package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

public class AppExpression extends Expression {
  private final Expression myFunction;
  private final Expression myArgument;

  private AppExpression(Expression function, Expression argument) {
    myFunction = function;
    myArgument = argument;
  }

  public static Expression make(Expression function, Expression argument) {
    if (function.isInstance(LamExpression.class)) {
      LamExpression lamExpr = function.cast(LamExpression.class);
      SingleDependentLink var = lamExpr.getParameters();
      SingleDependentLink next = var.getNext();
      return (next.hasNext() ? new LamExpression(lamExpr.getResultSort(), next, lamExpr.getBody()) : lamExpr.getBody()).subst(var, argument);
    } else {
      return new AppExpression(function, argument);
    }
  }

  public Expression getFunction() {
    return myFunction;
  }

  public Expression getArgument() {
    return myArgument;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public boolean isWHNF() {
    return myFunction.isWHNF() && !myFunction.isInstance(LamExpression.class);
  }

  @Override
  public Expression getStuckExpression() {
    return myFunction.getStuckExpression();
  }
}
