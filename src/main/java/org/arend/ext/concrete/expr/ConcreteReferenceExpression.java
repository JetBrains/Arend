package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteLevel;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ConcreteReferenceExpression extends ConcreteExpression {
  @Nonnull ArendRef getReferent();
  @Nullable ConcreteLevel getPLevel();
  @Nullable ConcreteLevel getHLevel();
}
