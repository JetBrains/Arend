package org.arend.core.expr;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

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

  @Override
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
  public Decision isWHNF(boolean normalizing) {
    return myFunction.isInstance(LamExpression.class) ? Decision.NO : myFunction.isWHNF(normalizing);
  }

  @Override
  public Expression getStuckExpression() {
    return myFunction.getStuckExpression();
  }
}
