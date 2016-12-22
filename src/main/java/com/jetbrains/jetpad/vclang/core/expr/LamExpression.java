package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

public class LamExpression extends Expression {
  private final DependentLink myLink;
  private final Expression myBody;

  public LamExpression(DependentLink link, Expression body) {
    myLink = link;
    myBody = body;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  public Expression getBody() {
    return myBody;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }

  @Override
  public LamExpression toLam() {
    return this;
  }
}
