package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;

public interface CoreEvaluatingBinding extends CoreBinding {
  @Nonnull CoreExpression getExpression();
}
