package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

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
  public DependentLink getLastBinding() {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    return MatchResult.FAIL;
  }

  @Override
  public boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2) {
    return other instanceof EmptyPattern || other instanceof BindingPattern;
  }
}
