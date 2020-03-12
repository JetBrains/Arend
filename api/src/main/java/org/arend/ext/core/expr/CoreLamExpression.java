package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.jetbrains.annotations.NotNull;

public interface CoreLamExpression extends CoreExpression {
  @NotNull
  CoreParameter getParameters();
  @NotNull CoreExpression getBody();
}
