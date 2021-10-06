package org.arend.naming.renamer;

import org.arend.ext.variable.Variable;
import org.arend.term.concrete.Concrete;

import java.util.Map;

public class MapReferableRenamer extends ReferableRenamer {
  private final Map<Variable, Concrete.Expression> myMapper;

  public MapReferableRenamer(Map<Variable, Concrete.Expression> mapper) {
    myMapper = mapper;
  }

  @Override
  public Concrete.Expression getConcreteExpression(Variable variable) {
    Concrete.Expression expr = myMapper.get(variable);
    return expr != null ? expr : super.getConcreteExpression(variable);
  }
}
