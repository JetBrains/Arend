package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.ext.core.elimtree.CoreLeafElimTree;
import org.arend.util.Decision;

import javax.annotation.Nonnull;
import java.util.List;

public class LeafElimTree extends ElimTree implements CoreLeafElimTree {
  private final Expression myExpression;

  public LeafElimTree(DependentLink parameters, Expression expression) {
    super(parameters);
    myExpression = expression;
  }

  @Nonnull
  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    return Decision.NO;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return null;
  }
}
