package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

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
