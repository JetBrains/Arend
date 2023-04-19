package org.arend.ext.core.expr;

import org.jetbrains.annotations.NotNull;

public interface CoreStringExpression extends CoreExpression {
  @NotNull String getString();
}
