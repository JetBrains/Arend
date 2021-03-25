package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreTypeCoerceExpression extends CoreExpression {
  @NotNull CoreExpression getLHSType();
  @NotNull CoreExpression getRHSType();
  @NotNull CoreExpression getArgument();
  boolean isFromLeftToRight();
}
