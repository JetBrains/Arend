package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreAbsExpression {
  @Nullable CoreBinding getBinding();
  @Nonnull CoreExpression getExpression();
}
