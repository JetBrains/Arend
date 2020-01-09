package org.arend.ext.core.expr;

import javax.annotation.Nonnull;

public interface CoreAppExpression extends CoreExpression {
  @Nonnull CoreExpression getFunction();
  @Nonnull CoreExpression getArgument();
}
