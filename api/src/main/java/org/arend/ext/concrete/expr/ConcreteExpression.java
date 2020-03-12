package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

public interface ConcreteExpression extends ConcreteSourceNode {
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression argument);
  @NotNull ConcreteExpression appImp(@NotNull ConcreteExpression argument);
}
