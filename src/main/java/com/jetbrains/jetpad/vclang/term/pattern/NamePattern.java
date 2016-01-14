package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;

import java.util.Collections;
import java.util.Map;

public class NamePattern extends Pattern implements Abstract.NamePattern {
  private final DependentLink myLink;

  public NamePattern(DependentLink link) {
    myLink = link;
  }

  @Override
  public DependentLink getParameters() {
    return myLink;
  }

  @Override
  public Expression toExpression(Map<Binding, Expression> substs) {
    Expression result = substs.get(myLink);
    return result == null ? new ReferenceExpression(myLink) : result;
  }

  @Override
  public MatchResult match(Expression expr) {
    return new MatchOKResult(Collections.singletonList(expr));
  }

  @Override
  public String getName() {
    return myLink.getName();
  }
}
