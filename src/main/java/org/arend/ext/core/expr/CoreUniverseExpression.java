package org.arend.ext.core.expr;

import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;

public interface CoreUniverseExpression extends CoreExpression {
  @Nonnull CoreSort getSort();
}
