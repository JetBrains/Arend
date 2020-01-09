package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;

import javax.annotation.Nonnull;

public interface CoreSigmaExpression extends CoreExpression {
  @Nonnull CoreParameter getParameters();
}
