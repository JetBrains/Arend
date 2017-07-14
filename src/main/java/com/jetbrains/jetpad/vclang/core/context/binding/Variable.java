package com.jetbrains.jetpad.vclang.core.context.binding;

public interface Variable {
  default String getName() {
    return toString();
  }
}
