package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

public class PiExpression extends DependentTypeExpression {
  private final Expression myCodomain;

  @Deprecated
  public PiExpression(DependentLink link, Expression codomain) {
    super(LevelArguments.ZERO, link);
    assert link.hasNext();
    myCodomain = codomain;
  }

  public PiExpression(LevelArguments levelArguments, DependentLink link, Expression codomain) {
    super(levelArguments, link);
    assert link.hasNext();
    myCodomain = codomain;
  }

  public Expression getCodomain() {
    return myCodomain;
  }

  @Override
  public DependentLink getPiParameters() {
    return getParameters();
  }

  @Override
  public Expression getPiCodomain() {
    return myCodomain;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }

  @Override
  public PiExpression toPi() {
    return this;
  }
}
