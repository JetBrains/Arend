package org.arend.core.constructor;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.Expression;

import java.util.List;

public abstract class SingleConstructor extends Constructor {
  public SingleConstructor() {
    super(null, null);
  }

  public abstract int getLength();

  public abstract List<Expression> getMatchedArguments(Expression argument, boolean normalizing);
}
