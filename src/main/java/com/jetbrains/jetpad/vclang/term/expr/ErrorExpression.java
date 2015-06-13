package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class ErrorExpression extends Expression implements Abstract.ErrorExpression {
  private final Expression myExpr;
  private final VcError myError;

  public ErrorExpression(Expression expr, VcError error) {
    myExpr = expr;
    myError = error;
  }

  @Override
  public Expression getExpr() {
    return myExpr;
  }

  public VcError getError() {
    return myError;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitError(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }
}
