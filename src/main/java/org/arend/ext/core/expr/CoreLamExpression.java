package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;

import javax.annotation.Nonnull;

public interface CoreLamExpression extends CoreExpression {
  @Nonnull CoreParameter getParameters();
  @Nonnull CoreExpression getBody();
}
