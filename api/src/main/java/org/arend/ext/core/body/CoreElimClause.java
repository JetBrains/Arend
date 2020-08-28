package org.arend.ext.core.body;

import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreElimClause {
  @NotNull List<? extends CorePattern> getPatterns();
  @Nullable CoreExpression getExpression();

  @Nullable AbstractedExpression getAbstractedExpression();
}
