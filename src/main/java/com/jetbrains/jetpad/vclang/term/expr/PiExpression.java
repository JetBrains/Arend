package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.UniverseOld;
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
  public UniverseOld getUniverse() {
    UniverseOld universe = super.getUniverse();
    Expression type = myCodomain.getType();
    if (type != null) {
      type = type.normalize(NormalizeVisitor.Mode.WHNF);
    }
    if (!(type instanceof UniverseExpression) || universe == null) {
      return null;
    }
    UniverseOld codomainUniverse = ((UniverseExpression) type).getUniverse();
    UniverseOld prop = new UniverseOld.Type(0, UniverseOld.Type.PROP);
    return codomainUniverse.equals(prop) ? prop : universe.max(codomainUniverse);
  }
}
