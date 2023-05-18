package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteBoxExpression extends ConcreteExpression {
  @NotNull ConcreteExpression getExpression();
}
