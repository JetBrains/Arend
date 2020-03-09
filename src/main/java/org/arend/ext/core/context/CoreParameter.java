package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface CoreParameter {
  @Contract(pure = true) boolean isExplicit();
  @NotNull CoreBinding getBinding();
  @NotNull CoreExpression getTypeExpr();
  @Contract(pure = true) @NotNull CoreParameter getNext();
}
