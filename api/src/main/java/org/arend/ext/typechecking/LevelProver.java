package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LevelProver {
  /**
   * Proves that an expression belongs to a given homotopy level.
   *
   * @param expression  an expression that should belong to the given level
   * @param type        the expected type of the result
   * @param level       the level to which the expression should belong, always >= -1
   * @param marker      a marker for errors
   * @return            a proof that the expression belongs to the given level
   */
  @Nullable TypedExpression prove(@NotNull CoreExpression expression, @NotNull CoreExpression type, int level, @NotNull ConcreteSourceNode marker, @NotNull ExpressionTypechecker typechecker);
}
