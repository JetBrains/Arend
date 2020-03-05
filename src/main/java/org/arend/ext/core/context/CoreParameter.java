package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

public interface CoreParameter {
  boolean isExplicit();
  @NotNull CoreBinding getBinding();
  @NotNull CoreExpression getTypeExpr();
  @NotNull CoreParameter getNext();
}
