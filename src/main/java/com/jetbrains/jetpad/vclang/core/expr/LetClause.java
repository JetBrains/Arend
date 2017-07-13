package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.NamedBinding;

public class LetClause extends NamedBinding {
  private Expression myExpression;

  public LetClause(String name, Expression expression) {
    super(name);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expression) {
    myExpression = expression;
  }

  @Override
  public Expression getTypeExpr() {
    return myExpression.getType();
  }
}
