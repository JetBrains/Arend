package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteLevel;
import org.jetbrains.annotations.Nullable;

public interface ConcreteUniverseExpression extends ConcreteExpression {
  @Nullable ConcreteLevel getPLevel();
  @Nullable ConcreteLevel getHLevel();
}
