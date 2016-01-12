package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class UniverseExpression extends Expression {
  private final Universe myUniverse;

  public UniverseExpression(Universe universe) {
    myUniverse = universe;
  }

  public Universe getUniverse() {
    return myUniverse;
  }

  @Override
  public UniverseExpression getType(List<Binding> context) {
    return new UniverseExpression(myUniverse.succ());
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitUniverse(this, params);
  }
}
