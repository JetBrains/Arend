package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.List;

public class EmptyPattern implements Pattern {
  public final static EmptyPattern INSTANCE = new EmptyPattern();

  private EmptyPattern() {}

  @Override
  public Expression toExpression() {
    return null;
  }

  @Override
  public DependentLink getFirstBinding() {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    return MatchResult.FAIL;
  }
}
