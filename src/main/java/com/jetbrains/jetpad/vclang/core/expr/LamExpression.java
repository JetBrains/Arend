package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;

public class LamExpression extends Expression {
  private final Level myPLevel;
  private final SingleDependentLink myLink;
  private final Expression myBody;

  public LamExpression(Level pLevel, SingleDependentLink link, Expression body) {
    myPLevel = pLevel;
    myLink = link;
    myBody = body;
  }

  public Level getPLevel() {
    return myPLevel;
  }

  public SingleDependentLink getParameters() {
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
