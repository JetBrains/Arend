package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Universe;

public abstract class DependentTypeExpression extends Expression {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  public Universe getUniverse() {
    DependentLink link = myLink;
    Universe universe = null;

    while (link.hasNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        Expression type = link.getType().getType();
        if (!(type instanceof UniverseExpression)) return null;
        Universe universe1 = ((UniverseExpression) type).getUniverse();
        if (universe == null) {
          universe = universe1;
        } else {
          Universe.CompareResult cmp = universe.compare(universe1, null);
          if (cmp == null) return null;
          universe = cmp.MaxUniverse;
        }
      }
      link = link.getNext();
    }

    return universe;
  }

  @Override
  public Expression getType() {
    Universe universe = getUniverse();
    return universe == null ? null : new UniverseExpression(universe);
  }

  @Override
  public DependentTypeExpression toDependentType() {
    return this;
  }
}
