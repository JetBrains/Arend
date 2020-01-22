package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ConcretePattern extends ConcreteSourceNode {
  @Nonnull ConcretePattern implicit();
  @Nonnull ConcretePattern as(@Nonnull ArendRef ref, @Nullable ConcreteExpression type);
}
