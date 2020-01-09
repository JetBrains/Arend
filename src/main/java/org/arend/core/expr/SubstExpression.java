package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class SubstExpression extends Expression {
  private final Expression myExpression;
  private final ExprSubstitution mySubstitution;

  private SubstExpression(Expression expression, ExprSubstitution substitution) {
    myExpression = expression;
    mySubstitution = substitution;
  }

  public static Expression make(Expression expression, ExprSubstitution substitution) {
    if (substitution.isEmpty()) {
      return expression;
    }
    if (expression instanceof SubstExpression) {
      ExprSubstitution newSubstitution = new ExprSubstitution(((SubstExpression) expression).mySubstitution);
      newSubstitution.subst(substitution);
      newSubstitution.addAll(substitution);
      substitution = newSubstitution;
    }
    return new SubstExpression(expression, substitution);
  }

  public Expression getExpression() {
    return myExpression;
  }

  public ExprSubstitution getSubstitution() {
    return mySubstitution;
  }

  public Expression getSubstExpression() {
    return myExpression.subst(mySubstitution);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSubst(this, params);
  }

  @Nonnull
  @Override
  public Expression getUnderlyingExpression() {
    return getSubstExpression().getUnderlyingExpression();
  }

  @Override
  public boolean isInstance(Class clazz) {
    return getSubstExpression().isInstance(clazz);
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return getSubstExpression().cast(clazz);
  }

  @Override
  public Decision isWHNF() {
    return getSubstExpression().isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return getSubstExpression().getStuckExpression();
  }
}
