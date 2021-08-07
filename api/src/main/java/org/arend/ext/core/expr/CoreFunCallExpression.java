package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.level.CoreLevels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreFunCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreFunctionDefinition getDefinition();
  @NotNull CoreLevels getLevels();
  @Nullable CoreExpression evaluate();
}
