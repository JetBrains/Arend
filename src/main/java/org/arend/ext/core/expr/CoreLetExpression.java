package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreEvaluatingBinding;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CoreLetExpression extends CoreExpression {
  @NotNull CoreExpression getExpression();
  @NotNull Collection<? extends CoreEvaluatingBinding> getClauses();
}
