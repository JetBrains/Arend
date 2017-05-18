package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;

public abstract class ElimTree {
  private DependentLink myParameters;

  ElimTree(DependentLink parameters) {
    myParameters = parameters;
  }

  public DependentLink getParameters() {
    return myParameters;
  }
}
