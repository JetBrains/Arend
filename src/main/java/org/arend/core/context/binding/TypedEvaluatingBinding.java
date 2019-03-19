package org.arend.core.context.binding;

import org.arend.core.expr.Expression;

public class TypedEvaluatingBinding extends TypedBinding implements EvaluatingBinding {
  private final Expression myExpression;

  public TypedEvaluatingBinding(String name, Expression expression, Expression type) {
    super(name, type);
    myExpression = expression;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }
}
