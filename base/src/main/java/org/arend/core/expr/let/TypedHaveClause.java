package org.arend.core.expr.let;

import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;

public class TypedHaveClause extends HaveClause {
  public Expression type;

  protected TypedHaveClause(String name, LetClausePattern pattern, Expression expression, Expression type) {
    super(name, pattern, expression);
    this.type = type;
  }

  @Override
  public Expression getTypeExpr() {
    return type == null || type instanceof ClassCallExpression ? super.getTypeExpr() : type;
  }
}
