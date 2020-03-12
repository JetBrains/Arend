package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreFunCallExpression extends CoreExpression {
  @NotNull CoreFunctionDefinition getDefinition();
  @NotNull
  CoreSort getSortArgument();
  @NotNull List<? extends CoreExpression> getDefCallArguments();
}
