package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nullable;

public interface CoreBinding {
  @Nullable String getName();
  CoreExpression getTypeExpr();
}
