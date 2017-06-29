package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;

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
}
