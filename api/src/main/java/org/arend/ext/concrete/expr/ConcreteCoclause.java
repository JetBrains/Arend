package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteCoclause extends ConcreteSourceNode {
  @NotNull ArendRef getImplementedRef();
  @Nullable ConcreteExpression getImplementation();
  @Nullable ConcreteCoclauses getSubCoclauses();
}
