package org.arend.core.constructor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.Expression;
import org.arend.core.elimtree.BranchKey;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.List;

public abstract class SingleConstructor implements BranchKey {
  public abstract List<Expression> getMatchedArguments(Expression argument, boolean normalizing);

  public abstract boolean compare(SingleConstructor other, Equations equations, Concrete.SourceNode sourceNode);

  @Override
  public DependentLink getParameters(ConstructorExpressionPattern pattern) {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public Body getBody() {
    return null;
  }
}
