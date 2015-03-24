package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;

public class ErrorExpression extends HoleExpression {
  private final TypeCheckingError myError;

  public ErrorExpression(Expression expr, TypeCheckingError error) {
    super(expr);
    myError = error;
  }

  public TypeCheckingError error() {
    return myError;
  }

  @Override
  public ErrorExpression getInstance(Expression expr) {
    return new ErrorExpression(expr, myError);
  }
}
