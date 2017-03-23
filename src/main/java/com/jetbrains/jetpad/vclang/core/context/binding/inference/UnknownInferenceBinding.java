package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;

public class UnknownInferenceBinding extends TypedBinding {
  public UnknownInferenceBinding(String name, Type type) {
    super(name, type);
  }
}
