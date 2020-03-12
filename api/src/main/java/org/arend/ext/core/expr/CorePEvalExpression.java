package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CorePEvalExpression extends CoreExpression {
  @NotNull CoreExpression getExpression();
}
