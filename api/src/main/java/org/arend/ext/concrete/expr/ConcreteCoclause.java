package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteCoclause {
  @NotNull ArendRef getImplementedRef();
  @Nullable ConcreteExpression getImplementation();
  @NotNull List<? extends ConcreteCoclause> getSubCoclauses();
}
