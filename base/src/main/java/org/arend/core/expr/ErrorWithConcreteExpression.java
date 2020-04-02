package org.arend.core.expr;

import org.arend.term.concrete.Concrete;

public class ErrorWithConcreteExpression extends ErrorExpression {
  public final Concrete.Expression expression;

  public ErrorWithConcreteExpression(Concrete.Expression expression) {
    this.expression = expression;
  }
}
