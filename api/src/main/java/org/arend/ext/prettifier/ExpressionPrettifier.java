package org.arend.ext.prettifier;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExpressionPrettifier {
  @Nullable ConcreteExpression prettify(@NotNull CoreExpression expression, @NotNull ExpressionPrettifier defaultPrettifier);
}
