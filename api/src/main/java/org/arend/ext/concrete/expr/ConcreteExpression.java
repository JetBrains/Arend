package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ConcreteExpression extends ConcreteSourceNode {
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression argument, boolean isExplicit);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression argument);
  @NotNull ConcreteExpression app(@NotNull ConcreteArgument argument);
  @NotNull ConcreteExpression app(@NotNull Collection<? extends ConcreteArgument> arguments);
}
