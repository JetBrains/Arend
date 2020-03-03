package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreNewExpression extends CoreExpression {
  @Nullable CoreExpression getRenewExpression();
  @NotNull CoreClassCallExpression getClassCall();

  @NotNull @Override CoreClassCallExpression getType();
}
