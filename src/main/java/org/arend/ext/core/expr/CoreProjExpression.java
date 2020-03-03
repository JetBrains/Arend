package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreProjExpression {
  @NotNull CoreExpression getExpression();
  int getField();
}
