package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;

import java.util.List;
import java.util.Map;

public interface Pattern {
  Expression toExpression();
  DependentLink getFirstBinding();
  DependentLink getLastBinding();
  MatchResult match(Expression expression, List<Expression> result);
  boolean unify(ExprSubstitution idpSubst, Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode);
  Pattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, Pattern> patternSubst);

  default Expression toPatternExpression() {
    return toExpression();
  }

  enum MatchResult { OK, MAYBE, FAIL }
}
