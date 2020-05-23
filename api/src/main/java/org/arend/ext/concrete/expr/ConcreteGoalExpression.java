package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.Nullable;

public interface ConcreteGoalExpression extends ConcreteExpression {
  @Nullable ConcreteExpression getExpression();
  @Nullable String getName();
}
