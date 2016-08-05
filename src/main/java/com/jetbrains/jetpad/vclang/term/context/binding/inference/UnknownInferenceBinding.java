package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class UnknownInferenceBinding extends TypedBinding {
  public UnknownInferenceBinding(String name, Expression type) {
    super(name, type);
  }
}
