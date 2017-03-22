package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.Set;

public class UniverseExpression extends Expression implements Type {
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
  public boolean equals(Object obj) {
    if (!(obj instanceof UniverseExpression)) {
      return false;
    }
    UniverseExpression expr = (UniverseExpression)obj;
    return mySort.equals(expr.getSort());
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return mySort.succ();
  }

  @Override
  public UniverseExpression subst(LevelSubstitution substitution) {
    return new UniverseExpression(mySort.subst(substitution));
  }

  @Override
  public UniverseExpression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return this;
  }
}
