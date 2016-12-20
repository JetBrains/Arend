package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class UniverseExpression extends Expression {
  private final Sort mySort;

  public UniverseExpression(Sort sort) {
    assert !sort.isOmega();
    mySort = sort;
  }

  public Sort getSort() {
    return mySort;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitUniverse(this, params);
  }

  @Override
  public UniverseExpression toUniverse() {
    return this;
  }

  @Override
  public boolean isAnyUniverse() {
    return mySort.getPLevel().isInfinity();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof UniverseExpression)) {
      return false;
    }
    UniverseExpression expr = (UniverseExpression)obj;
    return mySort.equals(expr.getSort());
  }
}
