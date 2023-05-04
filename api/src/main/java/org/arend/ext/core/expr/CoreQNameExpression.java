package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreDefinition;
import org.jetbrains.annotations.NotNull;

public interface CoreQNameExpression extends CoreExpression {
  @NotNull CoreDefinition getDefinition();
}
