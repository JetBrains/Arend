package org.arend.ext.core.elimtree;

import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CoreIntervalElim extends CoreBody {
  interface CasePair {
    @Nullable CoreExpression getLeftCase();
    @Nullable CoreExpression getRightCase();
  }

  @NotNull Collection<? extends CasePair> getCases();
  @Nullable CoreElimBody getOtherwise();
}
