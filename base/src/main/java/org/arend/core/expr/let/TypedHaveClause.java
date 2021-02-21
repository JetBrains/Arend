package org.arend.core.expr.let;

import org.arend.core.expr.Expression;

public class TypedHaveClause extends HaveClause {
  public final Expression type;

  protected TypedHaveClause(String name, LetClausePattern pattern, Expression expression, Expression type) {
    super(name, pattern, expression);
    this.type = type;
  }
}
