package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ArendRef;
import org.arend.ext.concrete.ConcreteLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConcreteReferenceExpression extends ConcreteExpression {
  @NotNull ArendRef getReferent();
  @Nullable ConcreteLevel getPLevel();
  @Nullable ConcreteLevel getHLevel();
}
