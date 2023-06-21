package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreLevels;
import org.jetbrains.annotations.NotNull;

public interface CorePathExpression extends CoreExpression {
  @NotNull CoreLevels getLevels();
  @NotNull CoreExpression getArgumentType();
  @NotNull CoreExpression getArgument();
}
