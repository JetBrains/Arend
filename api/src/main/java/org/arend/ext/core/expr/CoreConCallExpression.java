package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.definition.CoreConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreConCallExpression extends CoreExpression {
  @NotNull CoreConstructor getDefinition();
  @NotNull CoreSort getSortArgument();
  @NotNull List<? extends CoreExpression> getDataTypeArguments();
  @NotNull List<? extends CoreExpression> getDefCallArguments();
}
