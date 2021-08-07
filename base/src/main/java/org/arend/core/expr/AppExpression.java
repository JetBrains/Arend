package org.arend.core.expr;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreAppExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class AppExpression extends Expression implements CoreAppExpression {
  private final Expression myFunction;
  private final Expression myArgument;
  private final boolean myExplicit;

  private AppExpression(Expression function, Expression argument, boolean isExplicit) {
    myFunction = function;
    myArgument = argument;
    myExplicit = isExplicit;
  }

  public static Expression make(Expression function, Expression argument, boolean isExplicit) {
    function = function.getUnderlyingExpression();
    if (function instanceof LamExpression) {
      LamExpression lamExpr = (LamExpression) function;
      SingleDependentLink var = lamExpr.getParameters();
      SingleDependentLink next = var.getNext();
      return (next.hasNext() ? new LamExpression(lamExpr.getResultSort(), next, lamExpr.getBody()) : lamExpr.getBody()).subst(var, argument);
    } else {
      return new AppExpression(function, argument, isExplicit);
    }
  }

  @NotNull
  @Override
  public Expression getFunction() {
    return myFunction;
  }

  @NotNull
  @Override
  public Expression getArgument() {
    return myArgument;
  }

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  @Override
  public Expression getArguments(List<Expression> args) {
    assert args.isEmpty();
    AppExpression app = this;
    Expression fun = this;
    for (; app != null; app = fun.cast(AppExpression.class)) {
      args.add(app.getArgument());
      fun = app.getFunction();
    }
    Collections.reverse(args);
    return fun;
  }

  @Override
  public boolean canBeConstructor() {
    return myFunction.canBeConstructor();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitApp(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public Decision isWHNF() {
    return myFunction.isInstance(LamExpression.class) ? Decision.NO : myFunction.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myFunction.getStuckExpression();
  }
}
