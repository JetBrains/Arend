package org.arend.ext.core.expr;

import javax.annotation.Nonnull;

public interface CorePEvalExpression extends CoreExpression {
  @Nonnull CoreExpression getExpression();
}
