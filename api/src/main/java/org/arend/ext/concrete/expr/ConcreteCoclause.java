package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteClassElement;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConcreteCoclause extends ConcreteClassElement {
  @NotNull ArendRef getImplementedRef();
  @Nullable ConcreteExpression getImplementation();
  @Nullable ArendRef getClassReference();
  @Nullable ConcreteCoclauses getSubCoclauses();
}
