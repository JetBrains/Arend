package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreAtExpression extends CoreExpression {
  @NotNull CoreExpression getPathArgument();
  @NotNull CoreExpression getIntervalArgument();
}
