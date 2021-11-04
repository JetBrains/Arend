package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.jetbrains.annotations.NotNull;

public interface CoreTypeDestructorExpression extends CoreExpression {
  @NotNull CoreFunctionDefinition getDefinition();
  @NotNull CoreExpression getArgument();
}
