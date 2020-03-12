package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

public interface CheckedExpression {
  @NotNull
  CoreExpression getExpression();
  @NotNull CoreExpression getType();
}
