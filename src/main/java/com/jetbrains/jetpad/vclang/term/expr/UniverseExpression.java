package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class UniverseExpression extends Expression {
  private final Universe myUniverse;

  public UniverseExpression(Universe universe) {
    myUniverse = universe;
  }

  public Universe getUniverse() {
    return myUniverse;
  }

  @Override
  public UniverseExpression getType() {
    return new UniverseExpression(myUniverse.succ());
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
    return myUniverse instanceof TypeUniverse && ((TypeUniverse) myUniverse).getLevel() == null;
  }
}
