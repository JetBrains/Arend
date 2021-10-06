package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteParameter extends ConcreteSourceNode {
  boolean isExplicit();
  @NotNull List<? extends ArendRef> getRefList();
  @Nullable ConcreteExpression getType();

  @NotNull ConcreteParameter implicit();
  int getNumberOfParameters();
}
