package org.arend.ext.core.elimtree;

import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface CoreIntervalElim extends CoreBody {
  interface CasePair {
    @Nullable CoreExpression getLeftCase();
    @Nullable CoreExpression getRightCase();
  }

  @Nonnull Collection<? extends CasePair> getCases();
  @Nullable CoreElimTree getOtherwise();
}
