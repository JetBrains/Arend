package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class NewExpression extends Expression {
  private final Expression myExpression;

  public NewExpression(Expression expression) {
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression getType(List<Binding> context) {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }
}
