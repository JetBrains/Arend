package com.jetbrains.jetpad.vclang.core.context.binding;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

public interface Binding extends Variable {
  String getName();
  Expression getType();

  class Helper {
    public static String toString(Binding binding) {
      Expression type = binding.getType();
      return (binding.getName() == null ? "_" : binding.getName()) + (type == null ? "" : " : " + type);
    }
  }
}
