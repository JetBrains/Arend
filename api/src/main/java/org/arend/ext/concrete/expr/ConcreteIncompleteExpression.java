package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.Nullable;

public interface ConcreteIncompleteExpression extends ConcreteGoalExpression {
  @Override
  default @Nullable ConcreteExpression getExpression() {
    return null;
  }

  @Override
  default @Nullable String getName() {
    return null;
  }
}
