package org.arend.ext.concrete.pattern;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcretePattern extends ConcreteSourceNode {
  boolean isExplicit();
  @NotNull List<? extends ConcretePattern> getPatterns();
  @Nullable ArendRef getAsRef();
  @Nullable ConcreteExpression getAsRefType();

  @NotNull ConcretePattern implicit();
  @NotNull ConcretePattern as(@NotNull ArendRef ref, @Nullable ConcreteExpression type);
}
