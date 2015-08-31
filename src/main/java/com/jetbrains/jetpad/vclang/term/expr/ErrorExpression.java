package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.List;

public class ErrorExpression extends Expression implements Abstract.ErrorExpression {
  private final Expression myExpr;
  private final TypeCheckingError myError;

  public ErrorExpression(Expression expr, TypeCheckingError error) {
    myExpr = expr;
    myError = error;
  }

  @Override
  public Expression getExpr() {
    return myExpr;
  }

  public TypeCheckingError getError() {
    return myError;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitError(this);
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }
}
