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
        UniverseExpression type = link.getType().getType().toUniverse();
        if (type == null) return null;
        universe = universe == null ? type.getUniverse() : universe.max(type.getUniverse());
        if (universe == null) return null;
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
