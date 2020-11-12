package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteClassExtExpression extends ConcreteExpression {
  @NotNull ConcreteExpression getBaseClassExpression();
  @NotNull ConcreteCoclauses getCoclauses();
}
