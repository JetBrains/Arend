package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

public interface CoreFieldCallExpression extends CoreExpression {
  @NotNull CoreClassField getDefinition();
  @NotNull CoreSort getSortArgument();
  @NotNull CoreExpression getArgument();
}
