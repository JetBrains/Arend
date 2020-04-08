package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a typed core expression.
 */
public interface TypedExpression {
  @NotNull CoreExpression getExpression();
  @NotNull CoreExpression getType();
}
