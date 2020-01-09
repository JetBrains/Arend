package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;

import javax.annotation.Nonnull;

public interface CorePiExpression extends CoreExpression {
  @Nonnull CoreParameter getParameters();
  @Nonnull CoreExpression getCodomain();
}
