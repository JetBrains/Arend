package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public Universe getUniverse() {
    Universe universe = super.getUniverse();
    Expression type = myCodomain.getType();
    return !(type instanceof UniverseExpression) || universe == null ? null : universe.max(((UniverseExpression) type).getUniverse());
  }

  public Expression applyExpressions(List<Expression> expressions) {
    Map<Binding, Expression> substs = new HashMap<>();
    DependentLink link = getParameters();
    for (Expression expression : expressions) {
      assert link != null;
      substs.put(link, expression);
      link = link.getNext();
    }
    Expression result = myCodomain.subst(substs);
    return link == null ? result : new PiExpression(link.subst(substs), result);
  }
}
