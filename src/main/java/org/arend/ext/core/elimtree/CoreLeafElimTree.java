package org.arend.ext.core.elimtree;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;

public interface CoreLeafElimTree extends CoreElimTree {
  @Nonnull CoreExpression getExpression();
}
