package org.arend.ext.typechecking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DefermentChecker {
  enum Result { DO_NOT_DEFER, BEFORE_LEVELS, AFTER_LEVELS }

  @Nullable Result check(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData data);
}
