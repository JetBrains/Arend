package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MetaDefinition {
  @Nullable
  default CheckedExpression invoke(@Nonnull TypecheckingSession session, @Nonnull ContextData contextData) {
    return null;
  }

  default CoreExpression getExpectedType() {
    return null;
  }

  @Nullable
  default CheckedExpression invokeLater(@Nonnull TypecheckingSession session) {
    return null;
  }
}
