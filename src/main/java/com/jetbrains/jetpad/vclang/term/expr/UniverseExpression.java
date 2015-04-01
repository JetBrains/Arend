package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class UniverseExpression extends Expression implements Abstract.UniverseExpression {
  private final int level;

  public UniverseExpression() {
    level = -1;
  }

  public UniverseExpression(int level) {
    this.level = level;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitUniverse(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitUniverse(this, params);
  }
}
