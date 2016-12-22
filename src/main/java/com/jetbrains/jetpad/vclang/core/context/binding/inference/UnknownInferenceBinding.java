package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

public class UnknownInferenceBinding extends TypedBinding {
  public UnknownInferenceBinding(String name, Expression type) {
    super(name, type);
  }
}
