package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class IgnoreBinding extends InferenceBinding {
  public IgnoreBinding(String name, Expression type) {
    super(name, type);
  }
}
