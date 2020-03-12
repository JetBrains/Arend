package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

public interface CoreUniverseExpression extends CoreExpression {
  @NotNull
  CoreSort getSort();
}
