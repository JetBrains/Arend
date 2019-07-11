package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

public class NewExpression extends Expression {
  private final ClassCallExpression myExpression;

  public NewExpression(ClassCallExpression expression) {
    myExpression = expression;
  }

  public ClassCallExpression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  @Override
  public ClassCallExpression getType() {
    return myExpression;
  }
}
