package com.jetbrains.jetpad.vclang.core.context.binding;

import com.jetbrains.jetpad.vclang.core.expr.type.Type;

public interface Binding extends Variable {
  String getName();
  Type getType();

  class Helper {
    public static String toString(Binding binding) {
      return (binding.getName() == null ? "_" : binding.getName()) + (binding.getType() == null ? "" : " : " + binding.getType());
    }
  }
}
