package org.arend.ext.error;

import org.arend.ext.concrete.expr.ConcreteReferenceExpression;

public class IgnoredLevelsError extends TypecheckingError {
  public final ConcreteReferenceExpression expr;

  public IgnoredLevelsError(ConcreteReferenceExpression expr) {
    super(Level.WARNING_UNUSED, "Levels are ignored", expr);
    this.expr = expr;
  }
}
