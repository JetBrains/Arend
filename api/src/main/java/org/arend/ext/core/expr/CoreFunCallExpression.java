package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.jetbrains.annotations.NotNull;

public interface CoreFunCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreFunctionDefinition getDefinition();
}
