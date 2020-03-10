package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface CoreReferenceExpression extends CoreExpression {
  @Contract(pure = true) @NotNull CoreBinding getBinding();
}
