package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class PiExpression extends DependentTypeExpression {
  private final Expression myCodomain;

  public PiExpression(DependentLink link, Expression codomain) {
    super(link);
    myCodomain = codomain;
  }

  public Expression getCodomain() {
    return myCodomain;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }

  @Override
  public Universe getUniverse(List<Binding> context) {
    Universe universe = super.getUniverse(context);
    Expression type = myCodomain.getType(context);
    return !(type instanceof UniverseExpression) || universe == null ? null : universe.max(((UniverseExpression) type).getUniverse());
  }
}
