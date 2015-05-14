package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class InferHoleExpression extends Expression implements Abstract.InferHoleExpression {
  private final TypeCheckingError myError;

  public InferHoleExpression(TypeCheckingError error) {
    myError = error;
  }

  public TypeCheckingError getError() {
    return myError;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitInferHole(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferHole(this, params);
  }
}
