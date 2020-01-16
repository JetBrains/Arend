package org.arend.ext.concrete;

import javax.annotation.Nonnull;

public interface ConcreteArgument {
  @Nonnull ConcreteExpression getExpression();
  boolean isExplicit();
}
