package org.arend.ext.core.ops;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ExpressionMapper {
  @Nullable CoreExpression map(@Nonnull CoreExpression expression);
}
