package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;

import java.util.List;
import java.util.Map;

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
  public DependentLink getLastBinding() {
    return myBinding;
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    if (result != null) {
      result.add(expression);
    }
    return MatchResult.OK;
  }

  @Override
  public boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2) {
    if (!(other instanceof EmptyPattern) && substitution1 != null) {
      substitution1.add(myBinding, other.toExpression());
    }
    return true;
  }

  @Override
  public Pattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, Pattern> patternSubst) {
    if (patternSubst == null) {
      return this;
    }
    Pattern result = patternSubst.get(myBinding);
    assert result != null;
    return result;
  }
}
