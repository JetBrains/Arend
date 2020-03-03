package org.arend.core.constructor;

import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.List;

// TODO[lang_ext]: Do not extend Constructor
public abstract class SingleConstructor extends Constructor {
  public SingleConstructor() {
    super(null, null);
    setParameters(EmptyDependentLink.getInstance());
  }

  public abstract List<Expression> getMatchedArguments(Expression argument, boolean normalizing);

  public abstract boolean compare(SingleConstructor other, Equations equations, Concrete.SourceNode sourceNode);
}
