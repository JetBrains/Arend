package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverseNew;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class UniverseExpression extends Expression {
  private final TypeUniverseNew myUniverse;

  public UniverseExpression(TypeUniverseNew universe) {
    myUniverse = universe;
  }

  public TypeUniverseNew getUniverse() {
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
    return myUniverse.getPLevel().isInfinity();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof UniverseExpression)) {
      return false;
    }
    UniverseExpression expr = (UniverseExpression)obj;
    return myUniverse.equals(expr.getUniverse());
  }
}
