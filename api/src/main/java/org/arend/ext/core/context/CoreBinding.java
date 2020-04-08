package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a local binding.
 */
public interface CoreBinding {
  @Nullable String getName();
  CoreExpression getTypeExpr();
}
