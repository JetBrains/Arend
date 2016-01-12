package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.param.TypedBinding;

public class InferenceBinding extends TypedBinding {
  public InferenceBinding(String name, Expression type) {
    super(name, type);
  }

  public InferenceBinding(Name name, Expression type) {
    super(name, type);
  }

  public boolean isInference() {
    return true;
  }
}
