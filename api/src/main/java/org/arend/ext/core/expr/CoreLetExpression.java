package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CoreLetExpression extends CoreExpression {
  @NotNull CoreExpression getExpression();
  @NotNull Collection<? extends CoreBinding> getClauses();
}
