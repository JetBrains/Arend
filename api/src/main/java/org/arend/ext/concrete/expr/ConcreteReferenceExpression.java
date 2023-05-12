package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ArendRef;
import org.arend.ext.concrete.level.ConcreteLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteReferenceExpression extends ConcreteExpression {
  @NotNull ArendRef getReferent();
  @Nullable List<? extends ConcreteLevel> getPLevels();
  @Nullable List<? extends ConcreteLevel> getHLevels();
}
