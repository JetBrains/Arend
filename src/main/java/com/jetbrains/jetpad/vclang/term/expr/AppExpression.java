package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class AppExpression extends Expression implements Abstract.AppExpression {
  private final Expression myFunction;
  private final ArgumentExpression myArgument;

  public AppExpression(Expression function, ArgumentExpression argument) {
    myFunction = function;
    myArgument = argument;
  }

  @Override
  public Expression getFunction() {
    return myFunction;
  }

  @Override
  public ArgumentExpression getArgument() {
    return myArgument;
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
