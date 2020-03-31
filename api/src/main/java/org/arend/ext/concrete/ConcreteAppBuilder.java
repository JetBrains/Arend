package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ConcreteAppBuilder {
  @NotNull ConcreteExpression build();
  @NotNull ConcreteAppBuilder app(@NotNull ConcreteExpression argument, boolean isExplicit);
  @NotNull ConcreteAppBuilder app(@NotNull ConcreteExpression argument);
  @NotNull ConcreteAppBuilder app(@NotNull ConcreteArgument argument);
  @NotNull ConcreteAppBuilder app(@NotNull Collection<? extends ConcreteArgument> arguments);
}
