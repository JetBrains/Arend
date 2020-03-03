package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;

public interface CoreParameter {
  boolean isExplicit();
  @Nonnull CoreBinding getBinding();
  @Nonnull CoreExpression getTypeExpr();
  @Nonnull CoreParameter getNext();
}
