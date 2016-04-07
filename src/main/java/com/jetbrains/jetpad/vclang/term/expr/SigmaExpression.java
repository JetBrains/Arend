package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class SigmaExpression extends DependentTypeExpression {
  public SigmaExpression(DependentLink link) {
    super(link);
    assert link != null;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSigma(this, params);
  }

  @Override
  public SigmaExpression toSigma() {
    return this;
  }
}
