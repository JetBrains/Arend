package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.jetbrains.annotations.NotNull;

public interface CoreSigmaExpression extends CoreExpression {
  @NotNull
  CoreParameter getParameters();
}
