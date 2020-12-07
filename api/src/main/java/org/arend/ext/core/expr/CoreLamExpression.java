package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.jetbrains.annotations.NotNull;

public interface CoreLamExpression extends CoreExpression {
  @NotNull CoreParameter getParameters();
  @NotNull CoreExpression getBody();
  @NotNull AbstractedExpression getAbstractedBody();

  /**
   * Drops first {@code n} parameters. {@code n} should be less than the number of parameters.
   */
  @NotNull CoreLamExpression dropParameters(int n);
}
