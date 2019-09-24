package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;

import javax.annotation.Nonnull;

public class ElimClause {
  public final @Nonnull DependentLink parameters;
  public final @Nonnull Expression expression;

  public ElimClause(@Nonnull DependentLink parameters, @Nonnull Expression expression) {
    this.parameters = parameters;
    this.expression = expression;
  }
}
