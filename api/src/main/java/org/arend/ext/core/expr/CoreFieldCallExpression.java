package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreClassField;
import org.jetbrains.annotations.NotNull;

public interface CoreFieldCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreClassField getDefinition();
  @NotNull CoreExpression getArgument();
}
