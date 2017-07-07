package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.HashMap;

public abstract class ElimTree implements Body {
  private DependentLink myParameters;

  ElimTree(DependentLink parameters) {
    myParameters = parameters;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ElimTree && CompareVisitor.compare(new HashMap<>(), DummyEquations.getInstance(), Equations.CMP.EQ, this, (ElimTree) obj);
  }
}
