package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class DefCallExpression extends Expression implements Abstract.DefCallExpression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
  }

  @Override
  public Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitDefCall(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefCall(this, params);
  }
}
