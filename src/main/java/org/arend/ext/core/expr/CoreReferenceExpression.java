package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;

import javax.annotation.Nonnull;

public interface CoreReferenceExpression extends CoreExpression {
  @Nonnull CoreBinding getBinding();
}
