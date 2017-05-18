package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;

public class BindingPattern implements Pattern {
  private final DependentLink myBinding;

  public BindingPattern(DependentLink binding) {
    myBinding = binding;
  }

  public DependentLink getBinding() {
    return myBinding;
  }
}
