package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

public class FunCallExpression extends DefCallExpression {
  public FunCallExpression(FunctionDefinition definition) {
    super(definition);
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    return Apps(this, thisExpr);
  }

  @Override
  public FunctionDefinition getDefinition() {
    return (FunctionDefinition) super.getDefinition();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitFunCall(this);
  }
}
