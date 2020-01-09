package org.arend.core.constructor;

import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.Expression;

import java.util.List;

public abstract class SingleConstructor extends Constructor {
  public SingleConstructor() {
    super(null, null);
    setParameters(EmptyDependentLink.getInstance());
  }

  public abstract List<Expression> getMatchedArguments(Expression argument, boolean normalizing);
}
