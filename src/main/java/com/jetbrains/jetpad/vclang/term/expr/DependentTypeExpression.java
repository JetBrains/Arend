package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.param.UntypedDependentLink;

import java.util.List;

public abstract class DependentTypeExpression extends Expression {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  public DependentLink getLink() {
    return myLink;
  }

  public Universe getUniverse(List<Binding> context) {
    DependentLink link = myLink;
    Universe universe = null;

    while (link != null) {
      if (!(link instanceof UntypedDependentLink)) {
        Expression type = link.getType().getType(context);
        if (!(type instanceof UniverseExpression)) return null;
        Universe universe1 = ((UniverseExpression) type).getUniverse();
        universe = universe == null ? universe1 : universe.max(universe1);
        if (universe == null) return null;
      }
      link = link.getNext();
    }

    return universe;
  }

  @Override
  public Expression getType(List<Binding> context) {
    Universe universe = getUniverse(context);
    return universe == null ? null : new UniverseExpression(universe);
  }
}
