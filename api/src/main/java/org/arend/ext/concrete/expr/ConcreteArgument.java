package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteArgument {
  @NotNull ConcreteExpression getExpression();
  boolean isExplicit();
}
