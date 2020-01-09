package org.arend.ext.core.expr;

import javax.annotation.Nonnull;

public interface CoreProjExpression {
  @Nonnull CoreExpression getExpression();
  int getField();
}
