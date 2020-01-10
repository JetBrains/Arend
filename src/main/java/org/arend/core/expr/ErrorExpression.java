package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreErrorExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.typechecking.error.local.LocalError;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class ErrorExpression extends Expression implements CoreErrorExpression {
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
  public boolean canBeConstructor() {
    return false;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitError(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }
}
