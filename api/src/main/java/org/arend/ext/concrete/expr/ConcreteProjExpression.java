package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteProjExpression extends ConcreteExpression {
  @NotNull ConcreteExpression getExpression();
  int getField();
}
