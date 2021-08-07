package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.level.CoreLevels;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreConCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreConstructor getDefinition();
  @NotNull CoreLevels getLevels();
  @NotNull List<? extends CoreExpression> getDataTypeArguments();
}
