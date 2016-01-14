package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class InferHoleExpression extends Expression {
  private final TypeCheckingError myError;

  public InferHoleExpression(TypeCheckingError error) {
    myError = error;
  }

  public TypeCheckingError getError() {
    return myError;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferHole(this, params);
  }

  @Override
  public Expression getType() {
    return null;
  }
}
