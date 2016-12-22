package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;

import java.util.Collections;

public class AnyConstructorPattern extends Pattern implements Abstract.AnyConstructorPattern {
  private final DependentLink myLink;

  public AnyConstructorPattern(DependentLink link) {
    assert link != null;
    myLink = link;
  }

  public DependentLink getLink() {
    return myLink;
  }

  @Override
  public DependentLink getParameters() {
    return myLink;
  }

  @Override
  public Expression toExpression(ExprSubstitution subst, LevelArguments polyParams) {
    Expression result = subst.get(myLink);
    return result == null ? new ReferenceExpression(myLink) : result;
  }

  @Override
  public MatchResult match(Expression expr, boolean normalize) {
    if ((normalize ? expr.normalize(NormalizeVisitor.Mode.WHNF) : expr).toConCall() == null) {
      return new MatchMaybeResult(this, expr);
    } else {
      return new MatchOKResult(Collections.singletonList(expr));
    }
  }
}
