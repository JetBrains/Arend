package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConcreteAppExpression extends ConcreteExpression {
  @NotNull ConcreteExpression getFunction();
  @NotNull List<? extends ConcreteArgument> getArguments();
}
