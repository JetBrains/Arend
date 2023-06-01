package org.arend.ext.typechecking;

import org.arend.ext.core.context.CoreEvaluatingBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a typed core expression.
 */
public interface TypedExpression {
  @NotNull CoreExpression getExpression();
  @NotNull CoreExpression getType();

  /**
   * Creates a binging that evaluates to this expression.
   */
  @NotNull CoreEvaluatingBinding makeEvaluatingBinding(@Nullable String name);

  /**
   * Replaces the type in the given typed expression.
   * The original type should be less than or equal to the new type.
   * Otherwise, {@code null} is returned.
   */
  @Nullable TypedExpression replaceType(@NotNull CoreExpression type);
  @NotNull TypedExpression normalizeType();
}
