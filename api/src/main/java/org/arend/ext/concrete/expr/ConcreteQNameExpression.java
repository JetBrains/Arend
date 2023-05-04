package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

public interface ConcreteQNameExpression extends ConcreteExpression {
  @NotNull ArendRef getReference();
}
