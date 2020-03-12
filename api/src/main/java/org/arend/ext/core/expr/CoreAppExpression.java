package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreAppExpression extends CoreExpression {
  @NotNull CoreExpression getFunction();
  @NotNull CoreExpression getArgument();
}
