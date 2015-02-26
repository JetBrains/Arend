package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class AppExpression extends Expression implements Abstract.AppExpression {
  private final Expression function;
  private final Expression argument;

  public AppExpression(Expression function, Expression argument) {
    this.function = function;
    this.argument = argument;
  }

  @Override
  public Expression getFunction() {
    return function;
  }

  @Override
  public Expression getArgument() {
    return argument;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof AppExpression)) return false;
    AppExpression other = (AppExpression)o;
    return function.equals(other.function) && argument.equals(other.argument);
  }

  @Override
  public String toString() {
    return "(" + function.toString() + ") (" + argument.toString() + ")";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitApp(this);
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitApp(this);
  }
}
