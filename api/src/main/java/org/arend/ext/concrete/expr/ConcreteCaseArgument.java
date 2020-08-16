package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConcreteCaseArgument {
  @NotNull ConcreteExpression getExpression();
  @Nullable ArendRef getAsRef();
  @Nullable ConcreteExpression getType();
  boolean isElim();
}
