package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreLevel;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreArrayExpression extends CoreExpression {
  @NotNull CoreLevel getPLevel();
  @NotNull CoreLevel getHLevel();
  @NotNull List<? extends CoreExpression> getElements();
  @NotNull CoreExpression getElementsType();
  @Nullable CoreExpression getTail();
}
