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
   * @param hint          an expression specified by the user
   * @param type          a type that should belong to the given level
   * @param expectedType  the expected type of the result
   * @param level         the level to which the expression should belong, always >= -1
   * @param marker        a marker for errors
   * @return              a proof that the expression belongs to the given level
   */
  @Nullable TypedExpression prove(@Nullable ConcreteExpression hint, @NotNull CoreExpression type, @NotNull CoreExpression expectedType, int level, @NotNull ConcreteSourceNode marker, @NotNull ExpressionTypechecker typechecker);
}
