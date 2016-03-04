package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.UniverseOld;

public abstract class DependentTypeExpression extends Expression {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  public UniverseOld getUniverse() {
    DependentLink link = myLink;
    UniverseOld universe = null;

    while (link.hasNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        Expression type = link.getType().getType();
        if (!(type instanceof UniverseExpression)) return null;
        UniverseOld universe1 = ((UniverseExpression) type).getUniverse();
        universe = universe == null ? universe1 : universe.max(universe1);
        if (universe == null) return null;
      }
      link = link.getNext();
    }

    return universe;
  }

  @Override
  public Expression getType() {
    UniverseOld universe = getUniverse();
    return universe == null ? null : new UniverseExpression(universe);
  }
}
