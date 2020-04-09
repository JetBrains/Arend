package org.arend.ext.core.ops;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.UncheckedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExpressionMapper {
  @Nullable UncheckedExpression map(@NotNull CoreExpression expression);
}
