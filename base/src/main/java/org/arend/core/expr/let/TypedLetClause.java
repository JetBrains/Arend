package org.arend.core.expr.let;

import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;

public class TypedLetClause extends LetClause {
  public Expression type;

  protected TypedLetClause(String name, LetClausePattern pattern, Expression expression, Expression type) {
    super(name, pattern, expression);
    this.type = type;
  }

  public static HaveClause make(boolean isLet, String name, LetClausePattern pattern, Expression expression, Expression type) {
    return isLet ? new TypedLetClause(name, pattern, expression, type) : new TypedHaveClause(name, pattern, expression, type);
  }

  @Override
  public Expression getTypeExpr() {
    return type == null || type instanceof ClassCallExpression ? super.getTypeExpr() : type;
  }
}
