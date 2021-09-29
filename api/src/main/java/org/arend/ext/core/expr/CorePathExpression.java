package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreLevels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CorePathExpression extends CoreExpression {
  @NotNull CoreLevels getLevels();
  /**
   * @return {@code null} if the path is non-dependent; otherwise, an expression of type {@code I -> \Type}.
   */
  @Nullable CoreExpression getArgumentType();
  @NotNull CoreExpression getArgument();
}
