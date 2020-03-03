package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

public interface CoreEvaluatingBinding extends CoreBinding {
  @NotNull CoreExpression getExpression();
}
