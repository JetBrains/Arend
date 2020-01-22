package org.arend.ext.typechecking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MetaDefinition {
  @Nullable
  default CheckedExpression invoke(@Nonnull ExpressionTypechecker typechecker, @Nonnull ContextData contextData) {
    return null;
  }
}
