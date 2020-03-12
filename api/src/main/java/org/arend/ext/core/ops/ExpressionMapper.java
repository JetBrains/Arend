package org.arend.ext.core.ops;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExpressionMapper {
  @Nullable
  CoreExpression map(@NotNull CoreExpression expression);
}
