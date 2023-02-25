package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

public interface ConcreteThisExpression extends ConcreteExpression {
  @Nullable ArendRef getReferent();
}
