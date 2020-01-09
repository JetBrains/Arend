package org.arend.ext.core.expr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreNewExpression extends CoreExpression {
  @Nullable CoreExpression getRenewExpression();
  @Nonnull CoreClassCallExpression getClassCall();

  @Nonnull @Override CoreClassCallExpression getType();
}
