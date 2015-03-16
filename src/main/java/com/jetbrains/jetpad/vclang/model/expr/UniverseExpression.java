package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class UniverseExpression extends Expression implements Abstract.UniverseExpression {
  private final Property<Integer> myLevel = new ValueProperty<>();

  @Override
  public int getLevel() {
    return myLevel.get();
  }

  public Property<Integer> level() {
    return myLevel;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitUniverse(this, params);
  }
}
