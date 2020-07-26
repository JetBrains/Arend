package org.arend.core.expr;

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
  private final LevelSubstitution myLevelSubstitution;

  private SubstExpression(Expression expression, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    myExpression = expression;
    mySubstitution = substitution;
    myLevelSubstitution = levelSubstitution;
  }

  public static Expression make(Expression expression, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    if (substitution.isEmpty() && levelSubstitution.isEmpty()) {
      return expression;
    }
    if (expression instanceof SubstExpression) {
      SubstExpression substExpr = (SubstExpression) expression;
      ExprSubstitution newSubstitution = new ExprSubstitution(substExpr.mySubstitution);
      newSubstitution.subst(substitution);
      newSubstitution.addAll(substitution);
      substitution = newSubstitution;
      levelSubstitution = substExpr.myLevelSubstitution.subst(levelSubstitution);
      expression = substExpr.getExpression();
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
    return myLevelSubstitution;
  }

  public Expression getSubstExpression() {
    return myExpression instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) myExpression).getSubstExpression() == null ? myExpression : myExpression.subst(mySubstitution, myLevelSubstitution);
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
    return myExpression instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) myExpression).getSubstExpression() == null ? this : myExpression.subst(mySubstitution, myLevelSubstitution).getUnderlyingExpression();
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
