package org.arend.core.expr;

import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.LetClausePattern;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreLetExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LetExpression extends Expression implements CoreLetExpression {
  private final boolean myStrict;
  private final List<HaveClause> myClauses;
  private final Expression myExpression;

  public LetExpression(boolean isStrict, List<HaveClause> clauses, Expression expression) {
    myStrict = isStrict;
    myClauses = clauses;
    myExpression = expression;
  }

  public boolean isStrict() {
    return myStrict;
  }

  @NotNull
  @Override
  public List<HaveClause> getClauses() {
    return myClauses;
  }

  public static Expression normalizeClauseExpression(LetClausePattern pattern, Expression expression) {
    expression = expression.normalize(NormalizationMode.WHNF);
    if (!pattern.isMatching()) {
      return expression;
    }

    TupleExpression tuple = expression.cast(TupleExpression.class);
    if (tuple != null) {
      if (tuple.getFields().size() != pattern.getPatterns().size()) {
        return expression;
      }

      List<Expression> fields = new ArrayList<>(tuple.getFields().size());
      for (int i = 0; i < pattern.getPatterns().size(); i++) {
        fields.add(normalizeClauseExpression(pattern.getPatterns().get(i), tuple.getFields().get(i)));
      }
      return new TupleExpression(fields, tuple.getSigmaType());
    }

    NewExpression newExpr = expression.cast(NewExpression.class);
    if (newExpr != null && pattern.getFields() != null && pattern.getFields().size() == pattern.getPatterns().size()) {
      ClassCallExpression classCall = newExpr.getClassCall();
      Map<ClassField, Expression> implementations = new HashMap<>();
      ClassCallExpression resultClassCall = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES);

      boolean someNotImplemented = false;
      for (int i = 0; i < pattern.getPatterns().size(); i++) {
        ClassField classField = pattern.getFields().get(i);
        Expression impl = classCall.getImplementationHere(classField, newExpr);
        if (impl != null) {
          implementations.put(classField, normalizeClauseExpression(pattern.getPatterns().get(i), impl));
          someNotImplemented = true;
        }
      }
      resultClassCall.copyImplementationsFrom(classCall);
      Expression renew = newExpr.getRenewExpression();
      return new NewExpression(renew == null ? null : someNotImplemented ? renew.normalize(NormalizationMode.WHNF) : renew, resultClassCall);
    }

    return expression;
  }

  public Expression getResult() {
    if (!myStrict) {
      ExprSubstitution substitution = new ExprSubstitution();
      for (HaveClause clause : myClauses) {
        if (!(clause instanceof LetClause)) {
          substitution.add(clause, normalizeClauseExpression(clause.getPattern(), clause.getExpression().subst(substitution)));
        }
      }
      return myExpression.subst(substitution);
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (HaveClause clause : myClauses) {
      substitution.add(clause, normalizeClauseExpression(clause.getPattern(), clause.getExpression().subst(substitution)));
    }
    return myExpression.subst(substitution);
  }

  @NotNull
  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitLet(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.NO;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
