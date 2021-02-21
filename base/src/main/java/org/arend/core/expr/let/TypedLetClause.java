package org.arend.core.expr.let;

import org.arend.core.expr.Expression;

public class TypedLetClause extends LetClause {
  public final Expression type;

  protected TypedLetClause(String name, LetClausePattern pattern, Expression expression, Expression type) {
    super(name, pattern, expression);
    this.type = type;
  }

  public static HaveClause make(boolean isLet, String name, LetClausePattern pattern, Expression expression, Expression type) {
    return isLet ? new TypedLetClause(name, pattern, expression, type) : new TypedHaveClause(name, pattern, expression, type);
  }
}
