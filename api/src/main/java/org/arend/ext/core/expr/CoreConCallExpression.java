package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreConCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreConstructor getDefinition();
  @NotNull List<? extends CoreExpression> getDataTypeArguments();
}
