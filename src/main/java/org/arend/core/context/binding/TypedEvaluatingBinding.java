package org.arend.core.context.binding;

import org.arend.core.expr.Expression;
import org.arend.core.subst.SubstVisitor;

public class TypedEvaluatingBinding extends TypedBinding implements EvaluatingBinding {
  private Expression myExpression;

  public TypedEvaluatingBinding(String name, Expression expression, Expression type) {
    super(name, type);
    myExpression = expression;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public void subst(SubstVisitor visitor) {
    myExpression = myExpression.accept(visitor, null);
  }
}
