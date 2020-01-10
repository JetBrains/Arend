package org.arend.core.expr;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreAppExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class AppExpression extends Expression implements CoreAppExpression {
  private final Expression myFunction;
  private final Expression myArgument;

  private AppExpression(Expression function, Expression argument) {
    myFunction = function;
    myArgument = argument;
  }

  public static Expression make(Expression function, Expression argument) {
    LamExpression lamExpr = function.cast(LamExpression.class);
    if (lamExpr != null) {
      SingleDependentLink var = lamExpr.getParameters();
      SingleDependentLink next = var.getNext();
      return (next.hasNext() ? new LamExpression(lamExpr.getResultSort(), next, lamExpr.getBody()) : lamExpr.getBody()).subst(var, argument);
    } else {
      return new AppExpression(function, argument);
    }
  }

  @Nonnull
  @Override
  public Expression getFunction() {
    return myFunction;
  }

  @Nonnull
  @Override
  public Expression getArgument() {
    return myArgument;
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
  public <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
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
