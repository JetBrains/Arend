package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;

import java.util.List;

public interface Pattern {
  Expression toExpression();
  DependentLink getFirstBinding();
  DependentLink getLastBinding();
  MatchResult match(Expression expression, List<Expression> result);
  boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2);

  default Expression toPatternExpression() {
    return toExpression();
  }

  enum MatchResult { OK, MAYBE, FAIL }
}
