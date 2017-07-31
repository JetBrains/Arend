package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;

import java.util.List;

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

  public abstract boolean isWHNF(List<? extends Expression> arguments);
  public abstract Expression getStuckExpression(List<? extends Expression> arguments, Expression expression);
}
