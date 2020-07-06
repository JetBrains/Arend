package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteStringExpression extends ConcreteExpression {
  @NotNull String getUnescapedString();
}
