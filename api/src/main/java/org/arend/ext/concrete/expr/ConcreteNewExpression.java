package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteNewExpression extends ConcreteExpression {
  @NotNull ConcreteExpression getExpression();
}
