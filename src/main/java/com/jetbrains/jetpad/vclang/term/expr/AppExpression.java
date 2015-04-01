package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class AppExpression extends Expression implements Abstract.AppExpression {
  private final Expression myFunction;
  private final Expression myArgument;
  private final boolean myExplicit;

  public AppExpression(Expression function, Expression argument, boolean isExplicit) {
    myFunction = function;
    myArgument = argument;
    myExplicit = isExplicit;
  }

  @Override
  public Expression getFunction() {
    return myFunction;
  }

  @Override
  public Expression getArgument() {
    return myArgument;
  }

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitApp(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }
}
