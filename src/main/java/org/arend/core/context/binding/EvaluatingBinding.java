package org.arend.core.context.binding;

import org.arend.core.expr.Expression;

public class EvaluatingBinding extends TypedBinding {
  private final Expression myExpression;

  public EvaluatingBinding(String name, Expression expression, Expression type) {
    super(name, type);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }
}
