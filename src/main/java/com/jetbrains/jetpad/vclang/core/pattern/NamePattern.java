package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;

public class NamePattern extends Pattern implements Abstract.NamePattern {
  private final DependentLink myLink;

  public NamePattern(DependentLink link) {
    assert link != null;
    myLink = link;
  }

  @Override
  public DependentLink getParameters() {
    return myLink;
  }

  @Override
  public Expression toExpression(ExprSubstitution subst) {
    Expression result = subst.get(myLink);
    return result == null ? new ReferenceExpression(myLink) : result;
  }

  @Override
  public MatchResult match(Expression expr, boolean ignore) {
    return new MatchOKResult(Collections.singletonList(expr));
  }

  @Override
  public String getName() {
    return myLink.getName();
  }

  @Override
  public Abstract.ReferableSourceNode getReferent() {
    return new Abstract.ReferableSourceNode() {
      @Override
      public String getName() {
        return myLink.getName();
      }
    };
  }
}
