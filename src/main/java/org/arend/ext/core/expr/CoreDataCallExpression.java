package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreDataDefinition;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreDataCallExpression extends CoreExpression {
  @NotNull CoreDataDefinition getDefinition();
  @NotNull CoreSort getSortArgument();
  @NotNull List<? extends CoreExpression> getDefCallArguments();
}
