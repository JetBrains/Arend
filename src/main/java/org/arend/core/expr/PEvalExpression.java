package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

public class PEvalExpression extends Expression {
  private final FunCallExpression myExpression;

  public PEvalExpression(FunCallExpression expression) {
    myExpression = expression;
  }

  public FunCallExpression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPEval(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
