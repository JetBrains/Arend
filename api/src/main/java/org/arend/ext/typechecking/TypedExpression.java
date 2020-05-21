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

  @NotNull CoreEvaluatingBinding makeEvaluatingBinding(@Nullable String name);
}
