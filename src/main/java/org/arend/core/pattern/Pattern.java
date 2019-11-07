package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;

import java.util.List;
import java.util.Map;

public interface Pattern {
  Expression toExpression();
  DependentLink getFirstBinding();
  DependentLink getLastBinding();
  MatchResult match(Expression expression, List<Expression> result);
  boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2);
  Pattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, Pattern> patternSubst);

  enum MatchResult { OK, MAYBE, FAIL }
}
