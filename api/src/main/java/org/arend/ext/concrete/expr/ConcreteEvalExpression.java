package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

public interface ConcreteEvalExpression extends ConcreteExpression {
  boolean isPEval();
  @NotNull ConcreteExpression getExpression();
}
