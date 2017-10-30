package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

import java.util.List;

public interface Pattern {
  Expression toExpression();
  DependentLink getFirstBinding();
  DependentLink getLastBinding();
  MatchResult match(Expression expression, List<Expression> result);
  boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2);

  enum MatchResult { OK, MAYBE, FAIL }
}
