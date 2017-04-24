package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

public class LeafElimTree extends ElimTree {
  private Expression myExpression;

  public LeafElimTree(DependentLink parameters, Expression expression) {
    super(parameters);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }
}
