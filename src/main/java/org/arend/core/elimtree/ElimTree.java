package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.typechecking.implicitargs.equations.DummyEquations;

public abstract class ElimTree implements Body {
  private final DependentLink myParameters;

  ElimTree(DependentLink parameters) {
    myParameters = parameters;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ElimTree && CompareVisitor.compare(DummyEquations.getInstance(), this, (ElimTree) obj, null);
  }
}
