package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

public interface CoreParameter extends CoreBinding {
  boolean isExplicit();
  @NotNull @Override CoreExpression getTypeExpr();
  @NotNull CoreParameter getNext();
}
