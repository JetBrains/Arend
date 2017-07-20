package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.List;

public class LeafElimTree extends ElimTree {
  private final Expression myExpression;

  public LeafElimTree(DependentLink parameters, Expression expression) {
    super(parameters);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public boolean isWHNF(List<? extends Expression> arguments) {
    return false;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return null;
  }
}
