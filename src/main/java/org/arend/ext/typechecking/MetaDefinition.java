package org.arend.ext.typechecking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MetaDefinition {
  @Nullable CheckedExpression invoke(@Nonnull TypecheckingSession session, @Nonnull ContextData contextData);
}
