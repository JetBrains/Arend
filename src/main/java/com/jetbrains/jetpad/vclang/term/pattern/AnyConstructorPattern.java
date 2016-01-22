package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.NonDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.Collections;
import java.util.Map;

public class AnyConstructorPattern extends Pattern implements Abstract.AnyConstructorPattern {
  private final NonDependentLink myLink;

  public AnyConstructorPattern(NonDependentLink link) {
    myLink = link;
  }

  public NonDependentLink getLink() {
    return myLink;
  }

  @Override
  public DependentLink getParameters() {
    return myLink;
  }

  @Override
  public Expression toExpression(Substitution subst) {
    Expression result = subst.get(myLink);
    return result == null ? new ReferenceExpression(myLink) : result;
  }

  @Override
  public MatchResult match(Expression expr) {
    Expression func = expr.normalize(NormalizeVisitor.Mode.WHNF).getFunction(null);
    if (!(func instanceof ConCallExpression)) {
      return new MatchMaybeResult(this, expr);
    } else {
      return new MatchOKResult(Collections.singletonList(expr));
    }
  }
}
