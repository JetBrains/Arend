package org.arend.ext.reference;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.jetbrains.annotations.NotNull;

public interface ConcreteUnparsedSequenceElem {
  @NotNull ConcreteExpression getExpression();
  @NotNull Fixity getFixity();
  boolean isExplicit();
}
