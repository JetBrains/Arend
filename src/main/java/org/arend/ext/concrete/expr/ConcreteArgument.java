package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.expr.ConcreteExpression;

import javax.annotation.Nonnull;

public interface ConcreteArgument {
  @Nonnull
  ConcreteExpression getExpression();
  boolean isExplicit();
}
