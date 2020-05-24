package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LevelProver {
  /**
   * Proves that an expression belongs to a given homotopy level.
   *
   * @param expression    an expression that should belong to the given level
   * @param level         the level to which the expression should belong, always >= -1
   * @return              a proof that the expression belongs to the given level
   */
  @Nullable TypedExpression prove(@NotNull CoreExpression expression, int level, @NotNull ExpressionTypechecker typechecker);
}
