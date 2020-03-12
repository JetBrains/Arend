package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreAbsExpression {
  @Nullable
  CoreBinding getBinding();
  @NotNull CoreExpression getExpression();
}
