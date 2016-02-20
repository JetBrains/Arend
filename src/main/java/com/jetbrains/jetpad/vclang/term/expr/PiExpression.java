package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

public class PiExpression extends DependentTypeExpression {
  private final Expression myCodomain;

  public PiExpression(DependentLink link, Expression codomain) {
    super(link);
    assert link.hasNext();
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
  public Universe getUniverse() {
    Universe universe = super.getUniverse();
    Expression type = myCodomain.getType().normalize(NormalizeVisitor.Mode.WHNF);
    if (!(type instanceof UniverseExpression) || universe == null) {
      return null;
    }
    Universe codomainUniverse = ((UniverseExpression) type).getUniverse();
    Universe prop = new Universe.Type(0, Universe.Type.PROP);
    return codomainUniverse.equals(prop) ? prop : universe.max(codomainUniverse);
  }
}
