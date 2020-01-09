package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;

public interface CoreParameter extends CoreBinding {
  boolean isExplicit();
  @Nonnull @Override CoreExpression getTypeExpr();
  @Nonnull CoreParameter getNext();
}
