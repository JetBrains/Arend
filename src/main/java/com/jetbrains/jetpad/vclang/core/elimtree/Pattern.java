package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.List;

public interface Pattern {
  Expression toExpression();
  DependentLink getFirstBinding();
  MatchResult match(Expression expression, List<Expression> result);

  enum MatchResult { OK, MAYBE, FAIL }
}
