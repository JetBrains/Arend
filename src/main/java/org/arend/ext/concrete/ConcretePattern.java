package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConcretePattern extends ConcreteSourceNode {
  @NotNull ConcretePattern implicit();
  @NotNull ConcretePattern as(@NotNull ArendRef ref, @Nullable ConcreteExpression type);
}
