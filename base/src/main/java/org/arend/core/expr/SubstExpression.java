package org.arend.core.expr;

import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class SubstExpression extends Expression {
  private final Expression myExpression;
  private final ExprSubstitution mySubstitution;
  public LevelSubstitution levelSubstitution;

  public SubstExpression(Expression expression, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    myExpression = expression;
    mySubstitution = substitution;
    this.levelSubstitution = levelSubstitution;
  }

  public static Expression make(Expression expression, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    if (substitution.isEmpty() && levelSubstitution.isEmpty()) {
      return expression;
    }
    if (expression instanceof ReferenceExpression && !substitution.isEmpty()) {
      Expression result = substitution.get(((ReferenceExpression) expression).getBinding());
      return result != null ? result : expression;
    }
    return new SubstExpression(expression, substitution, levelSubstitution);
  }

  public Expression getExpression() {
    return myExpression;
  }

  public ExprSubstitution getSubstitution() {
    return mySubstitution;
  }

  public LevelSubstitution getLevelSubstitution() {
    return levelSubstitution;
  }

  public Expression getSubstExpression() {
    Expression expr = myExpression instanceof SubstExpression ? ((SubstExpression) myExpression).getSubstExpression() : myExpression;
    return expr instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr).getSubstExpression() == null ? expr : expr.subst(mySubstitution, levelSubstitution);
  }

  public Expression eval() {
    if (myExpression instanceof LetExpression) {
      ExprSubstitution substitution = new ExprSubstitution(mySubstitution);
      LetExpression let = (LetExpression) myExpression;
      if (let.isStrict()) {
        for (LetClause letClause : let.getClauses()) {
          substitution.add(letClause, LetExpression.normalizeClauseExpression(letClause.getPattern(), letClause.getExpression().subst(substitution, levelSubstitution)));
        }
      } else {
        for (LetClause letClause : let.getClauses()) {
          substitution.add(letClause, new ReferenceExpression(new LetClause(letClause.getName(), letClause.getPattern(), make(letClause.getExpression(), substitution, levelSubstitution))));
        }
      }
      return make(let.getExpression(), substitution, levelSubstitution);
    }

    Expression expr = myExpression instanceof SubstExpression ? ((SubstExpression) myExpression).getSubstExpression() : myExpression;
    return expr instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr).getSubstExpression() == null ? (expr == myExpression ? this : new SubstExpression(expr, mySubstitution, levelSubstitution)) : expr.subst(mySubstitution, levelSubstitution);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSubst(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitSubst(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return getSubstExpression().accept(visitor, params);
  }

  @NotNull
  @Override
  public Expression getUnderlyingExpression() {
    Expression expr = myExpression instanceof SubstExpression ? ((SubstExpression) myExpression).getSubstExpression() : myExpression;
    return expr instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr).getSubstExpression() == null ? (expr == myExpression ? this : new SubstExpression(expr, mySubstitution, levelSubstitution)) : expr.subst(mySubstitution, levelSubstitution).getUnderlyingExpression();
  }

  @Override
  public <T extends Expression> boolean isInstance(Class<T> clazz) {
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
