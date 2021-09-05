package org.arend.core.expr;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.inference.MetaInferenceVariable;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubstExpression extends Expression {
  private final Expression myExpression;
  private final ExprSubstitution mySubstitution;
  public LevelSubstitution levelSubstitution;

  private SubstExpression(Expression expression, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
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

    if (expression instanceof SubstExpression && ((SubstExpression) expression).isInferenceVariable()) {
      ExprSubstitution newSubst = new ExprSubstitution();
      for (Map.Entry<Binding, Expression> entry : ((SubstExpression) expression).getSubstitution().getEntries()) {
        newSubst.add(entry.getKey(), entry.getValue().subst(substitution, levelSubstitution));
      }
      InferenceReferenceExpression infRefExpr = (InferenceReferenceExpression) ((SubstExpression) expression).getExpression();
      for (Map.Entry<Binding, Expression> entry : substitution.getEntries()) {
        if (infRefExpr.getVariable().getBounds().contains(entry.getKey())) {
          newSubst.addIfAbsent(entry.getKey(), entry.getValue());
        }
      }
      return new SubstExpression(infRefExpr, newSubst, ((SubstExpression) expression).getLevelSubstitution().subst(levelSubstitution));
    }

    return new SubstExpression(expression, new ExprSubstitution(substitution), levelSubstitution);
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

  public boolean isInferenceVariable() {
    return myExpression instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) myExpression).getSubstExpression() == null;
  }

  public boolean isMetaInferenceVariable() {
    return myExpression instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) myExpression).getVariable() instanceof MetaInferenceVariable;
  }

  public Expression getSubstExpression() {
    Expression expr = myExpression instanceof SubstExpression ? ((SubstExpression) myExpression).getSubstExpression() : myExpression;
    return expr instanceof SubstExpression && expr == myExpression ? this : expr instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr).getSubstExpression() == null ? expr : expr.subst(mySubstitution, levelSubstitution);
  }

  public Expression eval() {
    if (myExpression instanceof LetExpression) {
      ExprSubstitution substitution = new ExprSubstitution(mySubstitution);
      LetExpression let = (LetExpression) myExpression;
      if (let.isStrict()) {
        for (HaveClause letClause : let.getClauses()) {
          substitution.add(letClause, LetExpression.normalizeClauseExpression(letClause.getPattern(), letClause.getExpression().subst(substitution, levelSubstitution)));
        }
      } else {
        for (HaveClause letClause : let.getClauses()) {
          substitution.add(letClause, letClause instanceof LetClause
            ? new ReferenceExpression(LetClause.make(true, letClause.getName(), letClause.getPattern(), make(letClause.getExpression(), substitution, levelSubstitution)))
            : LetExpression.normalizeClauseExpression(letClause.getPattern(), letClause.getExpression().subst(substitution, levelSubstitution)));
        }
      }
      return make(let.getExpression(), substitution, levelSubstitution);
    }

    if (myExpression instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) myExpression).getVariable() != null) {
      return this;
    }

    if (myExpression instanceof SubstExpression) {
      List<Pair<ExprSubstitution, LevelSubstitution>> substitutions = new ArrayList<>();
      Expression expr = this;
      while (expr instanceof SubstExpression) {
        substitutions.add(new Pair<>(((SubstExpression) expr).mySubstitution, ((SubstExpression) expr).levelSubstitution));
        expr = ((SubstExpression) expr).getExpression();
      }
      ExprSubstitution totalSubst = new ExprSubstitution();
      totalSubst.addAll(substitutions.get(substitutions.size() - 1).proj1);
      LevelSubstitution totalLevelSubst = substitutions.get(substitutions.size() - 1).proj2;
      for (int i = substitutions.size() - 2; i >= 0; i--) {
        totalSubst.addSubst(substitutions.get(i).proj1);
        totalLevelSubst = totalLevelSubst.subst(substitutions.get(i).proj2);
      }
      if (expr instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr).getVariable() != null) {
        return new SubstExpression(expr, totalSubst, totalLevelSubst);
      } else {
        return expr.subst(totalSubst, totalLevelSubst);
      }
    } else {
      return myExpression.subst(mySubstitution, levelSubstitution);
    }
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
    return clazz.isInstance(this) || getSubstExpression().isInstance(clazz);
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : isInferenceVariable() ? null : getSubstExpression().cast(clazz);
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
