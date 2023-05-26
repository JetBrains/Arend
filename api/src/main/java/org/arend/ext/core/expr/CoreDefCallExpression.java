package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreDefCallExpression extends CoreExpression {
  @NotNull CoreDefinition getDefinition();
  @NotNull List<? extends CoreExpression> getDefCallArguments();

  /**
   * If the type of the second parameter is the first parameter,
   * then this method creates a typed expression from the first two arguments of this expression.
   * Otherwise, it throws an exception.
   */
  @NotNull TypedExpression makeTypedExpression();
}
