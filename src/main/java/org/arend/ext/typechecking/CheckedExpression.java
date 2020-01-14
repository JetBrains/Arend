package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;

public interface CheckedExpression {
  @Nonnull CoreExpression getExpression();
  @Nonnull CoreExpression getType();
}
