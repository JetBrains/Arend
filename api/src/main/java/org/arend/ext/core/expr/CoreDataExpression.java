package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreDataExpression extends CoreExpression {
  @NotNull CoreExpression getExpression();
  @Nullable Object getMetaData();
}
