package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.subst.ExprSubstitution;

import java.util.List;

public class LetExpression extends Expression {
  private final List<LetClause> myClauses;
  private final Expression myExpression;

  public LetExpression(List<LetClause> clauses, Expression expression) {
    myClauses = clauses;
    myExpression = expression;
  }

  public List<LetClause> getClauses() {
    return myClauses;
  }

  public ExprSubstitution getClausesSubstitution() {
    ExprSubstitution substitution = new ExprSubstitution();
    for (LetClause clause : myClauses) {
      substitution.add(clause, clause.getExpression());
    }
    return substitution;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public boolean isWHNF() {
    return false;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
