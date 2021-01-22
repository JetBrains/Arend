package org.arend.ext.core.body;

import org.arend.ext.core.expr.CoreExpression;

public interface CoreExpressionPattern extends CorePattern {
  /**
   * @return  {@code null} if this pattern contains the absurd pattern;
   *          otherwise, returns the expression corresponding to this pattern.
   */
  CoreExpression toExpression();
}
