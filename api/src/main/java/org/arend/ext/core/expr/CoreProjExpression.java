package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreProjExpression extends CoreExpression {
  @NotNull CoreExpression getExpression();
  int getField();
}
