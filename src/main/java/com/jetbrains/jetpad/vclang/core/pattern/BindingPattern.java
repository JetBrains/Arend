package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

import java.util.List;

public class BindingPattern implements Pattern {
  private final DependentLink myBinding;

  public BindingPattern(DependentLink binding) {
    myBinding = binding;
  }

  public DependentLink getBinding() {
    return myBinding;
  }

  @Override
  public Expression toExpression() {
    return new ReferenceExpression(myBinding);
  }

  @Override
  public DependentLink getFirstBinding() {
    return myBinding;
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    result.add(expression);
    return MatchResult.OK;
  }

  @Override
  public boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2) {
    if (!(other instanceof EmptyPattern)) {
      substitution1.add(myBinding, other.toExpression());
    }
    return true;
  }
}
