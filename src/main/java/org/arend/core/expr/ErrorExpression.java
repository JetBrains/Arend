package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.typechecking.error.local.LocalError;
import org.arend.util.Decision;

public class ErrorExpression extends Expression {
  private final Expression myExpression;
  private final LocalError myError;

  public ErrorExpression(Expression expression, LocalError error) {
    myExpression = expression;
    myError = error;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public LocalError getError() {
    return myError;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public Decision isWHNF(boolean normalizing) {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }
}
