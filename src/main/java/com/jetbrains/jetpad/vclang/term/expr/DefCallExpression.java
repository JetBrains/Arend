package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class DefCallExpression extends Expression implements Abstract.DefCallExpression {
  private final Definition definition;

  public DefCallExpression(Definition function) {
    this.definition = function;
  }

  @Override
  public Definition getDefinition() {
    return definition;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof DefCallExpression)) return false;
    DefCallExpression other = (DefCallExpression)o;
    return definition.equals(other.definition);
  }

  @Override
  public String toString() {
    return definition.getName();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitDefCall(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefCall(this, params);
  }
}
