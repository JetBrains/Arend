package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteTypedExpression extends ConcreteExpression {
  @NotNull ConcreteExpression getExpression();
  @NotNull ConcreteExpression getType();
}
